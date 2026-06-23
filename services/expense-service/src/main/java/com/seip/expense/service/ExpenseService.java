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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    private final JdbcTemplate jdbcTemplate;
    private final ReceiptService receiptService;

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
        return withReceiptAccessUrls(expenseMapper.toDto(saved));
    }

    public ExpenseDto getExpenseById(Long id, Long employeeId) {
        Expense expense = findExpenseOrThrow(id);
        validateOwnership(expense, employeeId);
        return withReceiptAccessUrls(expenseMapper.toDto(expense));
    }

    public ExpenseDto getExpenseByIdForAuthorizedUser(Long requesterAuthUserId, String requesterRole, Long expenseId) {
        Expense expense = findExpenseOrThrow(expenseId);

        if (expense.getEmployeeId().equals(requesterAuthUserId)) {
            return withReceiptAccessUrls(expenseMapper.toDto(expense));
        }

        validateManagerScope(requesterAuthUserId, requesterRole, expense.getEmployeeId());
        return withReceiptAccessUrls(expenseMapper.toDto(expense));
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
        return withReceiptAccessUrls(expenseMapper.toDto(saved));
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

    public PageResponse<ExpenseSummaryDto> getPendingExpensesForManager(
            Long reviewerAuthUserId,
            String reviewerRole,
            Pageable pageable) {
        log.debug("Fetching pending expenses for reviewer {} with role {}", reviewerAuthUserId, reviewerRole);

        Page<Expense> page;
        if (isAdmin(reviewerRole)) {
            List<Long> employeeIds = getActiveEmployeeAuthUserIds();
            if (employeeIds.isEmpty()) {
                return PageResponse.of(Page.empty(pageable));
            }

            page = expenseRepository.findByEmployeeIdInAndStatusIn(
                    employeeIds,
                    List.of(ExpenseStatus.SUBMITTED, ExpenseStatus.UNDER_REVIEW),
                    pageable
            );
        } else if (isManager(reviewerRole)) {
            List<Long> departmentEmployeeIds = getDepartmentEmployeeAuthUserIds(reviewerAuthUserId);
            departmentEmployeeIds.remove(reviewerAuthUserId);

            if (departmentEmployeeIds.isEmpty()) {
                return PageResponse.of(Page.empty(pageable));
            }

            page = expenseRepository.findByEmployeeIdInAndStatusIn(
                    departmentEmployeeIds,
                    List.of(ExpenseStatus.SUBMITTED, ExpenseStatus.UNDER_REVIEW),
                    pageable
            );
        } else {
            throw new AccessDeniedException("Only managers or admins can review expenses");
        }

        Page<ExpenseSummaryDto> dtoPage = page.map(expenseMapper::toSummaryDto);
        return PageResponse.of(dtoPage);
    }

    public PageResponse<ExpenseSummaryDto> getExpensesForAuthorizedEmployee(
            Long requesterAuthUserId,
            String requesterRole,
            Long employeeAuthUserId,
            Pageable pageable) {
        validateManagerScope(requesterAuthUserId, requesterRole, employeeAuthUserId);
        Page<Expense> page = expenseRepository.findByEmployeeId(employeeAuthUserId, pageable);
        return PageResponse.of(page.map(expenseMapper::toSummaryDto));
    }

    @Transactional
    public ExpenseDto approveExpense(Long id, Long reviewerId, String reviewerRole, String notes) {
        log.info("Approving expense {} by reviewer {}", id, reviewerId);
        Expense expense = findExpenseOrThrow(id);
        validateReviewAccess(expense, reviewerId, reviewerRole);

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
        return withReceiptAccessUrls(expenseMapper.toDto(saved));
    }

    @Transactional
    public ExpenseDto rejectExpense(Long id, Long reviewerId, String reviewerRole, String notes) {
        log.info("Rejecting expense {} by reviewer {}", id, reviewerId);
        Expense expense = findExpenseOrThrow(id);
        validateReviewAccess(expense, reviewerId, reviewerRole);

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
        return withReceiptAccessUrls(expenseMapper.toDto(saved));
    }

    @Transactional
    public ExpenseDto requestChanges(Long id, Long reviewerId, String reviewerRole, String notes) {
        log.info("Requesting changes on expense {} by reviewer {}", id, reviewerId);
        Expense expense = findExpenseOrThrow(id);
        validateReviewAccess(expense, reviewerId, reviewerRole);

        if (expense.getStatus() != ExpenseStatus.SUBMITTED
                && expense.getStatus() != ExpenseStatus.UNDER_REVIEW) {
            throw new InvalidExpenseStateException(
                    "Changes can only be requested for submitted expenses. Current status: " + expense.getStatus());
        }

        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Review notes are required when requesting changes");
        }

        expense.setStatus(ExpenseStatus.UNDER_REVIEW);
        expense.setReviewerId(reviewerId);
        expense.setReviewNotes(notes);
        expense.setReviewedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        log.info("Changes requested for expense {} by reviewer {}", saved.getExpenseNumber(), reviewerId);
        return withReceiptAccessUrls(expenseMapper.toDto(saved));
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

    private void validateReviewAccess(Expense expense, Long reviewerAuthUserId, String reviewerRole) {
        if (expense.getEmployeeId().equals(reviewerAuthUserId)) {
            throw new AccessDeniedException("You cannot review your own expense");
        }

        if (isAdmin(reviewerRole)) {
            return;
        }

        if (!isManager(reviewerRole)) {
            throw new AccessDeniedException("Only managers or admins can review expenses");
        }

        if (!isEmployeeAuthUser(expense.getEmployeeId())) {
            throw new AccessDeniedException("Managers can only review employee expenses");
        }

        Long reviewerDepartmentId = getDepartmentIdForAuthUser(reviewerAuthUserId);
        Long employeeDepartmentId = getDepartmentIdForAuthUser(expense.getEmployeeId());

        if (reviewerDepartmentId == null || employeeDepartmentId == null
                || !reviewerDepartmentId.equals(employeeDepartmentId)) {
            throw new AccessDeniedException("You can only review expenses for employees in your department");
        }
    }

    private void validateManagerScope(Long requesterAuthUserId, String requesterRole, Long targetEmployeeAuthUserId) {
        if (isAdmin(requesterRole)) {
            return;
        }

        if (!isManager(requesterRole)) {
            throw new AccessDeniedException("Only managers or admins can access employee expense history");
        }

        if (!isEmployeeAuthUser(targetEmployeeAuthUserId)) {
            throw new AccessDeniedException("Managers can only access employee expense history");
        }

        Long requesterDepartmentId = getDepartmentIdForAuthUser(requesterAuthUserId);
        Long targetDepartmentId = getDepartmentIdForAuthUser(targetEmployeeAuthUserId);

        if (requesterDepartmentId == null || targetDepartmentId == null
                || !requesterDepartmentId.equals(targetDepartmentId)) {
            throw new AccessDeniedException("You can only access employees within your department");
        }
    }

    private List<Long> getDepartmentEmployeeAuthUserIds(Long authUserId) {
        Long departmentId = getDepartmentIdForAuthUser(authUserId);
        if (departmentId == null) {
            return new ArrayList<>();
        }

        List<Long> employeeIds = jdbcTemplate.queryForList("""
                SELECT e.auth_user_id
                FROM users.employees e
                JOIN auth.users u ON u.id = e.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = u.id
                JOIN auth.roles r ON r.id = ur.role_id
                WHERE e.department_id = ?
                  AND e.is_active = true
                  AND e.auth_user_id IS NOT NULL
                  AND u.enabled = true
                  AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                """, Long.class, departmentId);
        return new ArrayList<>(employeeIds != null ? employeeIds : Collections.emptyList());
    }

    private List<Long> getActiveEmployeeAuthUserIds() {
        List<Long> employeeIds = jdbcTemplate.queryForList("""
                SELECT e.auth_user_id
                FROM users.employees e
                JOIN auth.users u ON u.id = e.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = u.id
                JOIN auth.roles r ON r.id = ur.role_id
                WHERE e.is_active = true
                  AND e.auth_user_id IS NOT NULL
                  AND u.enabled = true
                  AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                """, Long.class);
        return new ArrayList<>(employeeIds != null ? employeeIds : Collections.emptyList());
    }

    private boolean isEmployeeAuthUser(Long authUserId) {
        List<Boolean> results = jdbcTemplate.query("""
                SELECT EXISTS (
                    SELECT 1
                    FROM auth.users u
                    JOIN auth.user_roles ur ON ur.user_id = u.id
                    JOIN auth.roles r ON r.id = ur.role_id
                    WHERE u.id = ?
                      AND u.enabled = true
                      AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                ) AS is_employee
                """, (rs, rowNum) -> rs.getBoolean("is_employee"), authUserId);

        return !results.isEmpty() && Boolean.TRUE.equals(results.get(0));
    }

    private Long getDepartmentIdForAuthUser(Long authUserId) {
        List<Long> results = jdbcTemplate.query("""
                SELECT department_id
                FROM users.employees
                WHERE auth_user_id = ?
                LIMIT 1
                """, (rs, rowNum) -> {
            long value = rs.getLong("department_id");
            return rs.wasNull() ? null : value;
        }, authUserId);

        return results.isEmpty() ? null : results.get(0);
    }

    private boolean isManager(String role) {
        return "ROLE_MANAGER".equals(normalizeRole(role));
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(normalizeRole(role));
    }

    private ExpenseDto withReceiptAccessUrls(ExpenseDto expenseDto) {
        if (expenseDto == null || expenseDto.getReceipts() == null || expenseDto.getReceipts().isEmpty()) {
            return expenseDto;
        }

        expenseDto.setReceipts(receiptService.withAccessUrls(expenseDto.getReceipts()));
        return expenseDto;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
