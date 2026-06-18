package com.seip.audit.controller;

import com.seip.audit.dto.ApiResponse;
import com.seip.audit.dto.AuditEventDto;
import com.seip.audit.dto.PageResponse;
import com.seip.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log query and compliance endpoints")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get all audit logs with optional filters",
               description = "Returns paginated audit logs. Supports filtering by userId, action keyword, and date range.")
    public ResponseEntity<ApiResponse<PageResponse<AuditEventDto>>> getLogs(
            @Parameter(description = "Page number (0-based)")  @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")              @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by userId")       @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by action keyword") @RequestParam(required = false) String action,
            @Parameter(description = "From timestamp (ISO)")   @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "To timestamp (ISO)")     @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        log.info("GET /audit/logs - page={}, size={}, userId={}, action={}", page, size, userId, action);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        PageResponse<AuditEventDto> result = auditService.getLogs(pageable, userId, action, from, to);

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", result));
    }

    @GetMapping("/logs/expense/{expenseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get audit logs by expense ID",
               description = "Returns all audit events related to a specific expense.")
    public ResponseEntity<ApiResponse<List<AuditEventDto>>> getLogsByExpenseId(
            @Parameter(description = "Expense ID") @PathVariable String expenseId) {

        log.info("GET /audit/logs/expense/{}", expenseId);
        List<AuditEventDto> result = auditService.getLogsByExpenseId(expenseId);
        return ResponseEntity.ok(ApiResponse.success(
                "Audit logs for expense " + expenseId + " retrieved successfully", result));
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get audit logs by user ID",
               description = "Returns paginated audit events performed by a specific user.")
    public ResponseEntity<ApiResponse<PageResponse<AuditEventDto>>> getLogsByUserId(
            @Parameter(description = "User ID")       @PathVariable String userId,
            @Parameter(description = "Page number")   @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")     @RequestParam(defaultValue = "20") int size) {

        log.info("GET /audit/logs/user/{}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        PageResponse<AuditEventDto> result = auditService.getLogsByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "Audit logs for user " + userId + " retrieved successfully", result));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get audit log by database ID")
    public ResponseEntity<ApiResponse<AuditEventDto>> getLogById(
            @Parameter(description = "Audit log DB ID") @PathVariable Long id) {

        log.info("GET /audit/logs/{}", id);
        AuditEventDto result = auditService.getLogById(id);
        return ResponseEntity.ok(ApiResponse.success("Audit log retrieved successfully", result));
    }
}
