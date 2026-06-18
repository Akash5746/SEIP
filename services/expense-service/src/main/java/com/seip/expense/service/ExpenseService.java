package com.seip.expense.service;

import com.seip.expense.dto.*;
import com.seip.expense.entity.*;
import com.seip.expense.exception.AccessDeniedException;
import com.seip.expense.exception.InvalidExpenseStateException;
import com.seip.expense.exception.ResourceNotFoundException;
import com.seip.expense.kafka.ExpenseEventPublisher;
import com.seip.expense.mapper.ExpenseMapper;
import com.seip.expense.repository.ExpenseRepository;
import com.seip.expense.util.ExpenseNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryService categoryService;
    private final ExpenseNumberGenerator expenseNumberGenerator;
    private final ExpenseEventPublisher eventPublisher;
    private final ExpenseMapper expenseMapper;

    @Transactional
    public ExpenseDto createExpense(Long employeeId, CreateExpenseRequest request) {
        log.info("Creating expense for employee: {}", employeeId);

        ExpenseCategory category = categoryService.findEntityById(request.getCategoryId());

        Expense expense = Expense.builder()
                .expenseNumber(expenseNumberGenerator.generateExpenseNumber())
                .employeeId(employeeId)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .merchantName(request.getMerchantName())
                .expenseDate(request.getExpenseDate())
                .status(ExpenseStatus.DRAFT)
                .riskScore(0)
                .riskLevel("LOW")
                .build();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<ExpenseItem> items = new ArrayList<>();
            for (ExpenseItemRequest itemReq : request.getItems()) {
                ExpenseItem item = ExpenseItem.builder()
                        .expense(expense)
                        .description(itemReq.getDescription())
                        .amount(itemReq.getAmount())
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build();
                items.add(item);
            }
            expense.setItems(items);
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Created expense {} for employee {}", saved.getExpenseNumber(), employeeId);
        return expenseMapper.toDto(saved);
    }

    public ExpenseDto getExpenseById(Long id, Long employeeId) {
        Expense expense = findExpenseOrThrow(id);
        validateOwnership(expense, employeeId);
        return expenseMapper.toDto(expense);
    }

    public PageResponse<ExpenseSummaryDto> getExpensesByEmployee(Long employeeId, Pageable pageable) {
        log.debug("Fetching expenses for employee: {}", employeeId);
        Page<Expense> page = expenseRepository.findByEmployeeId(employeeId, pageable);
        Page<ExpenseSummaryDto> dtoPage = page.map(expenseMapper::toSummaryDto);
        return PageResponse.of(dtoPage);
    }

    @Transactional
    public ExpenseDto updateExpense(Long id, Long employeeId, UpdateExpenseRequest request) {
        log.info("Updating expense {} for employee {}", id, employeeId);
        Expense expense = findExpenseOrThrow(id);
        validateOwnership(expense, employeeId);

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidExpenseStateException(
                    "Only DRAFT expenses can be updated. Current status: " + expense.getStatus());
        }

        if (request.getTitle() != null) {
            expense.setTitle(request.getTitle());
        }
        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        if (request.getCategoryId() != null) {
            ExpenseCategory category = categoryService.findEntityById(request.getCategoryId());
            expense.setCategory(category);
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getMerchantName() != null) {
            expense.setMerchantName(request.getMerchantName());
        }
        if (request.getCurrency() != null) {
            expense.setCurrency(request.getCurrency());
        }
        if (request.getItems() != null) {
            expense.getItems().clear();
            for (ExpenseItemRequest itemReq : request.getItems()) {
                ExpenseItem item = ExpenseItem.builder()
                        .expense(expense)
                        .description(itemReq.getDescription())
                        .amount(itemReq.getAmount())
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build();
                expense.getItems().add(item);
            }
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Updated expense {}", saved.getExpenseNumber());
        return expenseMapper.toDto(saved);
    }

    @Transactional
    public void deleteExpense(Long id, Long employeeId) {
        log.info("Deleting expense {} for employee {}", id, employeeId);
        Expense expense = findExpenseOrThrow(id);
        validateOwnership(expense, employeeId);

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidExpenseStateException(
                    "Only DRAFT expenses can be deleted. Current status: " + expense.getStatus());
        }

        expenseRepository.delete(expense);
        log.info("Deleted expense {}", expense.getExpenseNumber());
    }

    @Transactional
    public SubmitExpenseResponse submitExpense(Long id, Long employeeId) {
        log.info("Submitting expense {} for employee {}", id, employeeId);
        Expense expense = findExpenseOrThrow(id);
        validateOwnership(expense, employeeId);

        if (expense.getStatus() != ExpenseStatus.DRAFT) {
            throw new InvalidExpenseStateException(
                    "Only DRAFT expenses can be submitted. Current status: " + expense.getStatus());
        }

        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setSubmittedAt(LocalDateTime.now());
        Expense saved = expenseRepository.save(expense);

        // Publish events asynchronously
        eventPublisher.publishExpenseSubmitted(saved);
        eventPublisher.publishFraudCheckRequest(saved);

        log.info("Expense {} submitted successfully", saved.getExpenseNumber());
        return SubmitExpenseResponse.builder()
                .expenseNumber(saved.getExpenseNumber())
                .status(saved.getStatus())
                .message("Expense submitted successfully and is now under review")
                .build();
    }

    public PageResponse<ExpenseSummaryDto> getPendingExpensesForManager(Pageable pageable) {
        log.debug("Fetching pending expenses for manager review");
        Page<Expense> page = expenseRepository.findByStatusIn(
                List.of(ExpenseStatus.SUBMITTED, ExpenseStatus.UNDER_REVIEW), pageable);
        Page<ExpenseSummaryDto> dtoPage = page.map(expenseMapper::toSummaryDto);
        return PageResponse.of(dtoPage);
    }

    @Transactional
    public ExpenseDto approveExpense(Long id, Long reviewerId, String notes) {
        log.info("Approving expense {} by reviewer {}", id, reviewerId);
        Expense expense = findExpenseOrThrow(id);

        if (expense.getStatus() != ExpenseStatus.SUBMITTED
                && expense.getStatus() != ExpenseStatus.UNDER_REVIEW) {
            throw new InvalidExpenseStateException(
                    "Expense cannot be approved. Current status: " + expense.getStatus());
        }

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setReviewerId(reviewerId);
        expense.setReviewNotes(notes);
        expense.setReviewedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        eventPublisher.publishExpenseApproved(saved, reviewerId, notes);

        log.info("Expense {} approved by reviewer {}", saved.getExpenseNumber(), reviewerId);
        return expenseMapper.toDto(saved);
    }

    @Transactional
    public ExpenseDto rejectExpense(Long id, Long reviewerId, String notes) {
        log.info("Rejecting expense {} by reviewer {}", id, reviewerId);
        Expense expense = findExpenseOrThrow(id);

        if (expense.getStatus() != ExpenseStatus.SUBMITTED
                && expense.getStatus() != ExpenseStatus.UNDER_REVIEW) {
            throw new InvalidExpenseStateException(
                    "Expense cannot be rejected. Current status: " + expense.getStatus());
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setReviewerId(reviewerId);
        expense.setReviewNotes(notes);
        expense.setReviewedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        eventPublisher.publishExpenseRejected(saved, reviewerId, notes);

        log.info("Expense {} rejected by reviewer {}", saved.getExpenseNumber(), reviewerId);
        return expenseMapper.toDto(saved);
    }

    @Transactional
    public void updateRiskScore(Long expenseId, Integer score, String level) {
        log.info("Updating risk score for expense {}: score={}, level={}", expenseId, score, level);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        expense.setRiskScore(score);
        expense.setRiskLevel(level);
        expenseRepository.save(expense);
    }

    public boolean checkDuplicate(Long employeeId, BigDecimal amount, String merchantName, int withinDays) {
        LocalDate fromDate = LocalDate.now().minusDays(withinDays);
        boolean isDuplicate = expenseRepository.existsDuplicateExpense(
                employeeId, amount, merchantName, fromDate);
        log.debug("Duplicate check for employee {} amount {} merchant {}: {}", 
                employeeId, amount, merchantName, isDuplicate);
        return isDuplicate;
    }

    public int getMonthlyClaimCount(Long employeeId) {
        LocalDateTime startOfMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .atStartOfDay();
        LocalDateTime startOfNextMonth = LocalDate.now()
                .with(TemporalAdjusters.firstDayOfNextMonth())
                .atStartOfDay();
        return expenseRepository.countMonthlySubmissions(employeeId, startOfMonth, startOfNextMonth);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Expense findExpenseOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private void validateOwnership(Expense expense, Long employeeId) {
        if (!expense.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException(
                    "You do not have permission to access expense: " + expense.getExpenseNumber());
        }
    }
}
