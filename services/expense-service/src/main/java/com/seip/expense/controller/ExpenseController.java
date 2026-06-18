package com.seip.expense.controller;

import com.seip.expense.dto.*;
import com.seip.expense.service.ExpenseCategoryService;
import com.seip.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense Management", description = "APIs for managing employee expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseCategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a new expense in DRAFT status")
    public ResponseEntity<ApiResponse<ExpenseDto>> createExpense(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @Valid @RequestBody CreateExpenseRequest request) {
        log.info("POST /expenses - employeeId={}", employeeId);
        ExpenseDto dto = expenseService.createExpense(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Expense created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all expenses for the authenticated employee")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummaryDto>>> getMyExpenses(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ExpenseSummaryDto> response = expenseService.getExpensesByEmployee(employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all expense categories (alias for /categories)")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDto>>> getCategoriesAlias() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @GetMapping("/my")
    @Operation(summary = "Get expenses for the current employee (alias for GET /expenses)")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummaryDto>>> getMyExpensesAlias(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ExpenseSummaryDto> response = expenseService.getExpensesByEmployee(employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get pending expenses for approval (alias for /manager/queue)")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummaryDto>>> getPendingApprovalsAlias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String riskLevel) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").ascending());
        PageResponse<ExpenseSummaryDto> response = expenseService.getPendingExpensesForManager(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense details by ID")
    public ResponseEntity<ApiResponse<ExpenseDto>> getExpenseById(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long id) {
        ExpenseDto dto = expenseService.getExpenseById(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT expense")
    public ResponseEntity<ApiResponse<ExpenseDto>> updateExpense(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        ExpenseDto dto = expenseService.updateExpense(id, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Expense updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DRAFT expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long id) {
        expenseService.deleteExpense(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit an expense for review")
    public ResponseEntity<ApiResponse<SubmitExpenseResponse>> submitExpense(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long id) {
        SubmitExpenseResponse response = expenseService.submitExpense(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense submitted for review"));
    }

    @GetMapping("/manager/queue")
    @Operation(summary = "Get pending expenses for manager review (MANAGER/ADMIN only)")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseSummaryDto>>> getPendingQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").ascending());
        PageResponse<ExpenseSummaryDto> response = expenseService.getPendingExpensesForManager(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an expense (MANAGER/ADMIN only)")
    public ResponseEntity<ApiResponse<ExpenseDto>> approveExpense(
            @RequestHeader("X-Auth-User-Id") Long reviewerId,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewExpenseRequest request) {
        String notes = request != null ? request.getNotes() : null;
        ExpenseDto dto = expenseService.approveExpense(id, reviewerId, notes);
        return ResponseEntity.ok(ApiResponse.success(dto, "Expense approved"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an expense (MANAGER/ADMIN only)")
    public ResponseEntity<ApiResponse<ExpenseDto>> rejectExpense(
            @RequestHeader("X-Auth-User-Id") Long reviewerId,
            @PathVariable Long id,
            @Valid @RequestBody ReviewExpenseRequest request) {
        ExpenseDto dto = expenseService.rejectExpense(id, reviewerId, request.getNotes());
        return ResponseEntity.ok(ApiResponse.success(dto, "Expense rejected"));
    }

    @GetMapping("/check-duplicate")
    @Operation(summary = "Check for duplicate expense (internal use by fraud service)")
    public ResponseEntity<ApiResponse<Boolean>> checkDuplicate(
            @Parameter(description = "Employee ID") @RequestParam Long employeeId,
            @Parameter(description = "Expense amount") @RequestParam BigDecimal amount,
            @Parameter(description = "Merchant name") @RequestParam String merchantName,
            @Parameter(description = "Within last N days") @RequestParam(defaultValue = "30") int withinDays) {
        boolean isDuplicate = expenseService.checkDuplicate(employeeId, amount, merchantName, withinDays);
        return ResponseEntity.ok(ApiResponse.success(isDuplicate));
    }

    @GetMapping("/monthly-claim-count")
    @Operation(summary = "Get monthly claim count for employee (internal use by fraud service)")
    public ResponseEntity<ApiResponse<Integer>> getMonthlyClaimCount(
            @RequestParam Long employeeId) {
        int count = expenseService.getMonthlyClaimCount(employeeId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
