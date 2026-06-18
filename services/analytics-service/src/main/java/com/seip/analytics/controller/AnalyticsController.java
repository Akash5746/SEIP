package com.seip.analytics.controller;

import com.seip.analytics.dto.*;
import com.seip.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Cross-schema expense analytics and reporting endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get organisation-wide expense summary",
               description = "Returns totals, pending/approved/rejected breakdowns, and fraud risk overview.")
    public ResponseEntity<ApiResponse<OrgSummaryDto>> getOrgSummary() {
        log.info("GET /analytics/summary");
        OrgSummaryDto result = analyticsService.getOrgSummary();
        return ResponseEntity.ok(ApiResponse.success("Organisation summary retrieved successfully", result));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get monthly spend trends for a year",
               description = "Returns month-by-month total spend and expense count for the given year, excluding DRAFT/REJECTED.")
    public ResponseEntity<ApiResponse<List<MonthlySpendDto>>> getMonthlyTrends(
            @Parameter(description = "Year (e.g. 2024)") @RequestParam(defaultValue = "2024") int year) {
        log.info("GET /analytics/monthly?year={}", year);
        List<MonthlySpendDto> result = analyticsService.getMonthlyTrends(year);
        return ResponseEntity.ok(ApiResponse.success("Monthly trends retrieved successfully", result));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get category spend breakdown for a date range",
               description = "Returns total spend per category with percentage, sorted by amount descending.")
    public ResponseEntity<ApiResponse<List<CategorySpendDto>>> getCategoryBreakdown(
            @Parameter(description = "Start date (yyyy-MM-dd)")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd)")
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("GET /analytics/categories?from={}&to={}", from, to);
        List<CategorySpendDto> result = analyticsService.getCategoryBreakdown(from, to);
        return ResponseEntity.ok(ApiResponse.success("Category breakdown retrieved successfully", result));
    }

    @GetMapping("/employees/{employeeId}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get expense report for a specific employee",
               description = "Returns total, approved, rejected, and pending amounts for the given employee.")
    public ResponseEntity<ApiResponse<EmployeeReportDto>> getEmployeeReport(
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        log.info("GET /analytics/employees/{}/report", employeeId);
        EmployeeReportDto result = analyticsService.getEmployeeReport(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee report retrieved successfully", result));
    }

    @GetMapping("/departments/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get expense report for all departments",
               description = "Returns total spend, employee count, and average spend per employee per department.")
    public ResponseEntity<ApiResponse<List<DepartmentReportDto>>> getDepartmentReport() {
        log.info("GET /analytics/departments/all");
        List<DepartmentReportDto> result = analyticsService.getDepartmentReport();
        return ResponseEntity.ok(ApiResponse.success("Department report retrieved successfully", result));
    }

    @GetMapping("/fraud-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get fraud analysis trends for a year",
               description = "Returns monthly breakdown of total analyzed, HIGH/MEDIUM/LOW risk counts for fraud_schema.")
    public ResponseEntity<ApiResponse<List<FraudTrendDto>>> getFraudTrends(
            @Parameter(description = "Year (e.g. 2024)") @RequestParam(defaultValue = "2024") int year) {
        log.info("GET /analytics/fraud-trends?year={}", year);
        List<FraudTrendDto> result = analyticsService.getFraudTrends(year);
        return ResponseEntity.ok(ApiResponse.success("Fraud trends retrieved successfully", result));
    }

    @GetMapping("/top-spenders")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Get top N spenders",
               description = "Returns the top N employees by total approved/submitted expense amount.")
    public ResponseEntity<ApiResponse<List<TopSpenderDto>>> getTopSpenders(
            @Parameter(description = "Number of results (max 100)") @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /analytics/top-spenders?limit={}", limit);
        List<TopSpenderDto> result = analyticsService.getTopSpenders(limit);
        return ResponseEntity.ok(ApiResponse.success("Top spenders retrieved successfully", result));
    }
}
