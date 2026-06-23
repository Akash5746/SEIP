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

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Get dashboard stats for the current user")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats(
            @RequestHeader("X-Auth-User-Id") Long authUserId) {
        log.info("GET /analytics/dashboard-stats authUserId={}", authUserId);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved",
                analyticsService.getDashboardStatsForUser(authUserId)));
    }

    @GetMapping("/recent-expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Get recent expenses")
    public ResponseEntity<ApiResponse<List<RecentExpenseDto>>> getRecentExpenses(
            @RequestHeader("X-Auth-User-Id") Long authUserId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /analytics/recent-expenses authUserId={} limit={}", authUserId, limit);
        return ResponseEntity.ok(ApiResponse.success("Recent expenses retrieved",
                analyticsService.getRecentExpensesForUser(authUserId, limit)));
    }

    @GetMapping("/monthly-spend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Alias: monthly spend trends")
    public ResponseEntity<ApiResponse<List<MonthlySpendDto>>> getMonthlySpend(
            @RequestHeader("X-Auth-User-Id") Long authUserId,
            @RequestHeader("X-Auth-User-Role") String role,
            @RequestParam(defaultValue = "2024") int year) {
        log.info("GET /analytics/monthly-spend authUserId={} role={} year={}", authUserId, role, year);
        List<MonthlySpendDto> result = switch (role.toUpperCase()) {
            case "ROLE_MANAGER" -> analyticsService.getMonthlyTrendsForDepartment(authUserId, year);
            case "ROLE_ADMIN", "ROLE_FINANCE" -> analyticsService.getMonthlyTrends(year);
            default -> analyticsService.getMonthlyTrendsForUser(authUserId, year);
        };
        return ResponseEntity.ok(ApiResponse.success("Monthly spend retrieved", result));
    }

    @GetMapping("/category-spend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'EMPLOYEE')")
    @Operation(summary = "Alias: category spend breakdown")
    public ResponseEntity<ApiResponse<List<CategorySpendDto>>> getCategorySpend(
            @RequestHeader("X-Auth-User-Id") Long authUserId,
            @RequestHeader("X-Auth-User-Role") String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate f = from != null ? from : (startDate != null ? startDate : LocalDate.now().minusMonths(6));
        LocalDate t = to   != null ? to   : (endDate   != null ? endDate   : LocalDate.now());
        log.info("GET /analytics/category-spend authUserId={} role={} from={} to={}", authUserId, role, f, t);
        List<CategorySpendDto> result = switch (role.toUpperCase()) {
            case "ROLE_MANAGER" -> analyticsService.getCategoryBreakdownForDepartment(authUserId, f, t);
            case "ROLE_ADMIN", "ROLE_FINANCE" -> analyticsService.getCategoryBreakdown(f, t);
            default -> analyticsService.getCategoryBreakdownForUser(authUserId, f, t);
        };
        return ResponseEntity.ok(ApiResponse.success("Category spend retrieved", result));
    }

    @GetMapping("/department-spend")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    @Operation(summary = "Alias: department spend breakdown")
    public ResponseEntity<ApiResponse<List<DepartmentReportDto>>> getDepartmentSpend(
            @RequestHeader("X-Auth-User-Id") Long authUserId,
            @RequestHeader("X-Auth-User-Role") String role) {
        log.info("GET /analytics/department-spend authUserId={} role={}", authUserId, role);
        List<DepartmentReportDto> result = "ROLE_MANAGER".equals(role.toUpperCase())
                ? analyticsService.getDepartmentReportForManager(authUserId)
                : analyticsService.getDepartmentReport();
        return ResponseEntity.ok(ApiResponse.success("Department spend retrieved", result));
    }

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get manager dashboard metrics scoped to the manager's department")
    public ResponseEntity<ApiResponse<ManagerDashboardDto>> getManagerDashboard(
            @RequestHeader("X-Auth-User-Id") Long authUserId) {
        log.info("GET /analytics/manager/dashboard authUserId={}", authUserId);
        return ResponseEntity.ok(ApiResponse.success("Manager dashboard retrieved",
                analyticsService.getManagerDashboard(authUserId)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get organisation-wide expense summary",
               description = "Returns totals, pending/approved/rejected breakdowns, and fraud risk overview.")
    public ResponseEntity<ApiResponse<OrgSummaryDto>> getOrgSummary() {
        log.info("GET /analytics/summary");
        OrgSummaryDto result = analyticsService.getOrgSummary();
        return ResponseEntity.ok(ApiResponse.success("Organisation summary retrieved successfully", result));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
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
            @RequestHeader("X-Auth-User-Id") Long authUserId,
            @RequestHeader("X-Auth-User-Role") String role,
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        log.info("GET /analytics/employees/{}/report requesterAuthUserId={} role={}", employeeId, authUserId, role);
        EmployeeReportDto result = analyticsService.getAuthorizedEmployeeReport(authUserId, role, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee report retrieved successfully", result));
    }

    @GetMapping("/departments/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get expense report for all departments",
               description = "Returns total spend, employee count, and average spend per employee per department.")
    public ResponseEntity<ApiResponse<List<DepartmentReportDto>>> getDepartmentReport() {
        log.info("GET /analytics/departments/all");
        List<DepartmentReportDto> result = analyticsService.getDepartmentReport();
        return ResponseEntity.ok(ApiResponse.success("Department report retrieved successfully", result));
    }

    @GetMapping("/fraud-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get fraud analysis trends for a year",
               description = "Returns monthly breakdown of total analyzed, HIGH/MEDIUM/LOW risk counts for fraud_schema.")
    public ResponseEntity<ApiResponse<List<FraudTrendDto>>> getFraudTrends(
            @Parameter(description = "Year (e.g. 2024)") @RequestParam(defaultValue = "2024") int year) {
        log.info("GET /analytics/fraud-trends?year={}", year);
        List<FraudTrendDto> result = analyticsService.getFraudTrends(year);
        return ResponseEntity.ok(ApiResponse.success("Fraud trends retrieved successfully", result));
    }

    @GetMapping("/top-spenders")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get top N spenders",
               description = "Returns the top N employees by total approved/submitted expense amount.")
    public ResponseEntity<ApiResponse<List<TopSpenderDto>>> getTopSpenders(
            @Parameter(description = "Number of results (max 100)") @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /analytics/top-spenders?limit={}", limit);
        List<TopSpenderDto> result = analyticsService.getTopSpenders(limit);
        return ResponseEntity.ok(ApiResponse.success("Top spenders retrieved successfully", result));
    }
}
