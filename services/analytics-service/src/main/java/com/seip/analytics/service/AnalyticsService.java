package com.seip.analytics.service;

import com.seip.analytics.dto.*;
import com.seip.analytics.exception.AccessDeniedException;
import com.seip.analytics.exception.AnalyticsException;
import com.seip.analytics.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardStatsDto getDashboardStatsForUser(Long authUserId) {
        log.debug("Running user dashboard stats query for authUserId={}", authUserId);
        try {
            String sql = """
                SELECT
                    COUNT(*)                                                                    AS total_expenses,
                    COUNT(CASE WHEN status IN ('SUBMITTED','UNDER_REVIEW') THEN 1 END)         AS pending_count,
                    COALESCE(SUM(CASE WHEN DATE_TRUNC('month', expense_date) = DATE_TRUNC('month', CURRENT_DATE)
                                     THEN amount ELSE 0 END), 0)                               AS this_month_amount,
                    COUNT(CASE WHEN risk_level = 'HIGH' THEN 1 END)                            AS high_risk_alerts,
                    COUNT(CASE WHEN status = 'APPROVED'
                                AND DATE_TRUNC('month', reviewed_at) = DATE_TRUNC('month', CURRENT_DATE)
                                THEN 1 END)                                                    AS approved_this_month,
                    COUNT(CASE WHEN status = 'REJECTED'
                                AND DATE_TRUNC('month', reviewed_at) = DATE_TRUNC('month', CURRENT_DATE)
                                THEN 1 END)                                                    AS rejected_this_month
                FROM expense.expenses
                WHERE status != 'DRAFT'
                  AND employee_id = ?
                """;
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardStatsDto(
                    rs.getLong("total_expenses"),
                    rs.getLong("pending_count"),
                    rs.getDouble("this_month_amount"),
                    rs.getLong("high_risk_alerts"),
                    rs.getLong("approved_this_month"),
                    rs.getLong("rejected_this_month")
            ), authUserId);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute dashboard stats", e);
        }
    }

    public List<RecentExpenseDto> getRecentExpensesForUser(Long authUserId, int limit) {
        log.debug("Running user recent expenses query for authUserId={}, limit={}", authUserId, limit);
        return queryRecentExpenses("""
                SELECT id, title, amount, status,
                       COALESCE(risk_level, 'LOW') AS risk_level,
                       created_at, merchant_name
                FROM expense.expenses
                WHERE employee_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """, authUserId, sanitizeLimit(limit));
    }

    public List<MonthlySpendDto> getMonthlyTrendsForUser(Long authUserId, int year) {
        log.debug("Running user monthly trends query for authUserId={}, year={}", authUserId, year);
        return queryMonthlyTrends("""
                SELECT
                    EXTRACT(MONTH FROM e.expense_date)::INT AS month,
                    EXTRACT(YEAR  FROM e.expense_date)::INT AS year,
                    COALESCE(SUM(e.amount), 0)              AS total_amount,
                    COUNT(*)                                AS expense_count
                FROM expense.expenses e
                WHERE EXTRACT(YEAR FROM e.expense_date) = ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND e.employee_id = ?
                GROUP BY month, year
                ORDER BY month ASC
                """, year, authUserId);
    }

    public List<MonthlySpendDto> getMonthlyTrendsForDepartment(Long managerAuthUserId, int year) {
        Long departmentId = getDepartmentIdForAuthUser(managerAuthUserId);
        log.debug("Running department monthly trends query for managerAuthUserId={}, departmentId={}, year={}",
                managerAuthUserId, departmentId, year);
        return queryMonthlyTrends("""
                SELECT
                    EXTRACT(MONTH FROM e.expense_date)::INT AS month,
                    EXTRACT(YEAR  FROM e.expense_date)::INT AS year,
                    COALESCE(SUM(e.amount), 0)              AS total_amount,
                    COUNT(*)                                AS expense_count
                FROM expense.expenses e
                JOIN users.employees u ON u.auth_user_id = e.employee_id
                JOIN auth.users au ON au.id = u.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = au.id
                JOIN auth.roles ar ON ar.id = ur.role_id
                WHERE EXTRACT(YEAR FROM e.expense_date) = ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND u.department_id = ?
                  AND au.enabled = true
                  AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                GROUP BY month, year
                ORDER BY month ASC
                """, year, departmentId);
    }

    public List<MonthlySpendDto> getMonthlyTrends(int year) {
        log.debug("Running org monthly trends query for year={}", year);
        return queryMonthlyTrends("""
                SELECT
                    EXTRACT(MONTH FROM e.expense_date)::INT AS month,
                    EXTRACT(YEAR  FROM e.expense_date)::INT AS year,
                    COALESCE(SUM(e.amount), 0)              AS total_amount,
                    COUNT(*)                                AS expense_count
                FROM expense.expenses e
                WHERE EXTRACT(YEAR FROM e.expense_date) = ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY month, year
                ORDER BY month ASC
                """, year);
    }

    public List<CategorySpendDto> getCategoryBreakdownForUser(Long authUserId, LocalDate from, LocalDate to) {
        log.debug("Running user category breakdown for authUserId={} from={} to={}", authUserId, from, to);
        return queryCategoryBreakdown("""
                SELECT COALESCE(SUM(e.amount), 0)
                FROM expense.expenses e
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND e.employee_id = ?
                """, """
                SELECT
                    ec.name                            AS category_name,
                    ec.code                            AS category_code,
                    COALESCE(SUM(e.amount), 0)         AS total_amount,
                    COUNT(e.id)                        AS expense_count
                FROM expense.expenses e
                JOIN expense.expense_categories ec ON e.category_id = ec.id
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND e.employee_id = ?
                GROUP BY ec.name, ec.code
                ORDER BY total_amount DESC
                """, from, to, authUserId);
    }

    public List<CategorySpendDto> getCategoryBreakdownForDepartment(Long managerAuthUserId, LocalDate from, LocalDate to) {
        Long departmentId = getDepartmentIdForAuthUser(managerAuthUserId);
        log.debug("Running department category breakdown for managerAuthUserId={}, departmentId={}, from={}, to={}",
                managerAuthUserId, departmentId, from, to);
        return queryCategoryBreakdown("""
                SELECT COALESCE(SUM(e.amount), 0)
                FROM expense.expenses e
                JOIN users.employees u ON u.auth_user_id = e.employee_id
                JOIN auth.users au ON au.id = u.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = au.id
                JOIN auth.roles ar ON ar.id = ur.role_id
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND u.department_id = ?
                  AND au.enabled = true
                  AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                """, """
                SELECT
                    ec.name                            AS category_name,
                    ec.code                            AS category_code,
                    COALESCE(SUM(e.amount), 0)         AS total_amount,
                    COUNT(e.id)                        AS expense_count
                FROM expense.expenses e
                JOIN expense.expense_categories ec ON e.category_id = ec.id
                JOIN users.employees u ON u.auth_user_id = e.employee_id
                JOIN auth.users au ON au.id = u.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = au.id
                JOIN auth.roles ar ON ar.id = ur.role_id
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                  AND u.department_id = ?
                  AND au.enabled = true
                  AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                GROUP BY ec.name, ec.code
                ORDER BY total_amount DESC
                """, from, to, departmentId);
    }

    public List<CategorySpendDto> getCategoryBreakdown(LocalDate from, LocalDate to) {
        log.debug("Running org category breakdown from={} to={}", from, to);
        return queryCategoryBreakdown("""
                SELECT COALESCE(SUM(e.amount), 0)
                FROM expense.expenses e
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                """, """
                SELECT
                    ec.name                            AS category_name,
                    ec.code                            AS category_code,
                    COALESCE(SUM(e.amount), 0)         AS total_amount,
                    COUNT(e.id)                        AS expense_count
                FROM expense.expenses e
                JOIN expense.expense_categories ec ON e.category_id = ec.id
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY ec.name, ec.code
                ORDER BY total_amount DESC
                """, from, to);
    }

    public EmployeeReportDto getEmployeeReport(Long employeeAuthUserId) {
        log.debug("Running org employee report for employeeAuthUserId={}", employeeAuthUserId);
        return queryEmployeeReport(employeeAuthUserId);
    }

    public EmployeeReportDto getAuthorizedEmployeeReport(Long requesterAuthUserId, String requesterRole, Long employeeAuthUserId) {
        String normalizedRole = normalizeRole(requesterRole);
        if ("ROLE_MANAGER".equals(normalizedRole)) {
            ensureSameDepartment(requesterAuthUserId, employeeAuthUserId);
            ensureEmployeeRole(employeeAuthUserId);
        } else if (!List.of("ROLE_ADMIN", "ROLE_FINANCE").contains(normalizedRole)) {
            throw new AccessDeniedException("You do not have permission to view this employee report");
        }
        return queryEmployeeReport(employeeAuthUserId);
    }

    public List<DepartmentReportDto> getDepartmentReportForManager(Long managerAuthUserId) {
        Long departmentId = getDepartmentIdForAuthUser(managerAuthUserId);
        log.debug("Running manager department report for managerAuthUserId={}, departmentId={}", managerAuthUserId, departmentId);
        return jdbcTemplate.query("""
                SELECT
                    u.department_id                                       AS department_id,
                    COALESCE(d.name, 'Department')                        AS department_name,
                    COALESCE(SUM(e.amount), 0)                            AS total_amount,
                    COUNT(DISTINCT e.employee_id)                         AS employee_count,
                    CASE WHEN COUNT(DISTINCT e.employee_id) > 0
                         THEN COALESCE(SUM(e.amount), 0) / COUNT(DISTINCT e.employee_id)
                         ELSE 0
                    END                                                   AS avg_per_employee
                FROM expense.expenses e
                JOIN users.employees u ON u.auth_user_id = e.employee_id
                JOIN auth.users au ON au.id = u.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = au.id
                JOIN auth.roles ar ON ar.id = ur.role_id
                LEFT JOIN users.departments d ON d.id = u.department_id
                WHERE e.status NOT IN ('DRAFT', 'REJECTED')
                  AND u.department_id = ?
                  AND au.enabled = true
                  AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                GROUP BY u.department_id, d.name
                """, (rs, rowNum) -> new DepartmentReportDto(
                rs.getLong("department_id"),
                rs.getString("department_name"),
                rs.getBigDecimal("total_amount"),
                rs.getLong("employee_count"),
                rs.getBigDecimal("avg_per_employee")
        ), departmentId);
    }

    public List<DepartmentReportDto> getDepartmentReport() {
        log.debug("Running org department report query");
        return jdbcTemplate.query("""
                SELECT
                    u.department_id                                       AS department_id,
                    COALESCE(d.name, 'Department')                        AS department_name,
                    COALESCE(SUM(e.amount), 0)                            AS total_amount,
                    COUNT(DISTINCT e.employee_id)                         AS employee_count,
                    CASE WHEN COUNT(DISTINCT e.employee_id) > 0
                         THEN COALESCE(SUM(e.amount), 0) / COUNT(DISTINCT e.employee_id)
                         ELSE 0
                    END                                                   AS avg_per_employee
                FROM expense.expenses e
                JOIN users.employees u ON u.auth_user_id = e.employee_id
                JOIN auth.users au ON au.id = u.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = au.id
                JOIN auth.roles ar ON ar.id = ur.role_id
                LEFT JOIN users.departments d ON d.id = u.department_id
                WHERE e.status NOT IN ('DRAFT', 'REJECTED')
                  AND au.enabled = true
                  AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                GROUP BY u.department_id, d.name
                ORDER BY total_amount DESC
                """, (rs, rowNum) -> new DepartmentReportDto(
                rs.getLong("department_id"),
                rs.getString("department_name"),
                rs.getBigDecimal("total_amount"),
                rs.getLong("employee_count"),
                rs.getBigDecimal("avg_per_employee")
        ));
    }

    public ManagerDashboardDto getManagerDashboard(Long managerAuthUserId) {
        Long departmentId = getDepartmentIdForAuthUser(managerAuthUserId);
        String departmentName = jdbcTemplate.queryForObject(
                "SELECT name FROM users.departments WHERE id = ?",
                String.class,
                departmentId
        );

        try {
            return jdbcTemplate.queryForObject("""
                    SELECT
                        (
                            SELECT COUNT(DISTINCT ue.auth_user_id)
                            FROM users.employees ue
                            JOIN auth.users au ON au.id = ue.auth_user_id
                            JOIN auth.user_roles ur ON ur.user_id = au.id
                            JOIN auth.roles ar ON ar.id = ur.role_id
                            WHERE ue.department_id = ?
                              AND ue.is_active = true
                              AND ue.auth_user_id IS NOT NULL
                              AND au.enabled = true
                              AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                        )                                                                                       AS total_employees,
                        (
                            SELECT COUNT(*)
                            FROM expense.expenses e
                            JOIN users.employees ue ON ue.auth_user_id = e.employee_id
                            JOIN auth.users au ON au.id = ue.auth_user_id
                            JOIN auth.user_roles ur ON ur.user_id = au.id
                            JOIN auth.roles ar ON ar.id = ur.role_id
                            WHERE ue.department_id = ?
                              AND ue.is_active = true
                              AND au.enabled = true
                              AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                              AND e.status IN ('SUBMITTED','UNDER_REVIEW')
                        )                                                                                       AS pending_approvals,
                        (
                            SELECT COALESCE(SUM(e.amount), 0)
                            FROM expense.expenses e
                            JOIN users.employees ue ON ue.auth_user_id = e.employee_id
                            JOIN auth.users au ON au.id = ue.auth_user_id
                            JOIN auth.user_roles ur ON ur.user_id = au.id
                            JOIN auth.roles ar ON ar.id = ur.role_id
                            WHERE ue.department_id = ?
                              AND ue.is_active = true
                              AND au.enabled = true
                              AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                              AND DATE_TRUNC('month', e.expense_date) = DATE_TRUNC('month', CURRENT_DATE)
                              AND e.status NOT IN ('DRAFT', 'REJECTED')
                        )                                                                                       AS department_expenses_this_month,
                        (
                            SELECT COUNT(*)
                            FROM expense.expenses e
                            JOIN users.employees ue ON ue.auth_user_id = e.employee_id
                            JOIN auth.users au ON au.id = ue.auth_user_id
                            JOIN auth.user_roles ur ON ur.user_id = au.id
                            JOIN auth.roles ar ON ar.id = ur.role_id
                            WHERE ue.department_id = ?
                              AND ue.is_active = true
                              AND au.enabled = true
                              AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                              AND e.risk_level = 'HIGH'
                              AND e.status NOT IN ('DRAFT', 'REJECTED')
                        )                                                                                       AS high_risk_alerts,
                        (
                            SELECT COUNT(*)
                            FROM expense.expenses e
                            WHERE e.employee_id = ?
                              AND e.status != 'DRAFT'
                        )                                                                                       AS personal_expense_count,
                        (
                            SELECT COALESCE(SUM(e.amount), 0)
                            FROM expense.expenses e
                            WHERE e.employee_id = ?
                              AND DATE_TRUNC('month', e.expense_date) = DATE_TRUNC('month', CURRENT_DATE)
                              AND e.status != 'DRAFT'
                        )                                                                                       AS personal_this_month_amount
                    """, (rs, rowNum) -> new ManagerDashboardDto(
                    departmentName,
                    rs.getLong("total_employees"),
                    rs.getLong("pending_approvals"),
                    rs.getBigDecimal("department_expenses_this_month"),
                    rs.getLong("high_risk_alerts"),
                    rs.getLong("personal_expense_count"),
                    rs.getBigDecimal("personal_this_month_amount")
            ), departmentId, departmentId, departmentId, departmentId, managerAuthUserId, managerAuthUserId);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute manager dashboard", e);
        }
    }

    public OrgSummaryDto getOrgSummary() {
        log.debug("Running org summary query");
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT
                        COUNT(*)                                                          AS total_expenses,
                        COALESCE(SUM(e.amount), 0)                                       AS total_amount,
                        COALESCE(SUM(CASE WHEN e.status IN ('PENDING','SUBMITTED','UNDER_REVIEW')
                                         THEN e.amount ELSE 0 END), 0)                  AS pending_amount,
                        COALESCE(SUM(CASE WHEN e.status IN ('APPROVED','REIMBURSED')
                                         THEN e.amount ELSE 0 END), 0)                  AS approved_amount,
                        COALESCE(SUM(CASE WHEN e.status = 'REJECTED'
                                         THEN e.amount ELSE 0 END), 0)                  AS rejected_amount,
                        0                                                               AS avg_risk_score,
                        COUNT(CASE WHEN e.risk_level = 'HIGH' THEN 1 END)              AS high_risk_count
                    FROM expense.expenses e
                    """, (rs, rowNum) -> new OrgSummaryDto(
                    rs.getLong("total_expenses"),
                    rs.getBigDecimal("total_amount"),
                    rs.getBigDecimal("pending_amount"),
                    rs.getBigDecimal("approved_amount"),
                    rs.getBigDecimal("rejected_amount"),
                    rs.getDouble("avg_risk_score"),
                    rs.getLong("high_risk_count")
            ));
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute organisation summary", e);
        }
    }

    public List<FraudTrendDto> getFraudTrends(int year) {
        log.debug("Running fraud trends query for year={}", year);
        try {
            return jdbcTemplate.query("""
                    SELECT
                        EXTRACT(MONTH FROM fa.analysis_time)::INT   AS month,
                        EXTRACT(YEAR  FROM fa.analysis_time)::INT   AS year,
                        COUNT(*)                                     AS total_analyzed,
                        COUNT(CASE WHEN fa.risk_level = 'HIGH'   THEN 1 END) AS high_risk,
                        COUNT(CASE WHEN fa.risk_level = 'MEDIUM' THEN 1 END) AS medium_risk,
                        COUNT(CASE WHEN fa.risk_level = 'LOW'    THEN 1 END) AS low_risk
                    FROM fraud.fraud_analysis fa
                    WHERE EXTRACT(YEAR FROM fa.analysis_time) = ?
                    GROUP BY month, year
                    ORDER BY month ASC
                    """, (rs, rowNum) -> new FraudTrendDto(
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getLong("total_analyzed"),
                    rs.getLong("high_risk"),
                    rs.getLong("medium_risk"),
                    rs.getLong("low_risk")
            ), year);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute fraud trends for year " + year, e);
        }
    }

    public List<TopSpenderDto> getTopSpenders(int limit) {
        log.debug("Running top spenders query, limit={}", limit);
        int sanitizedLimit = sanitizeLimit(limit);
        try {
            return jdbcTemplate.query("""
                    SELECT
                        e.employee_id,
                        COALESCE(SUM(e.amount), 0) AS total_amount,
                        COUNT(e.id)                AS claim_count
                    FROM expense.expenses e
                    JOIN users.employees ue ON ue.auth_user_id = e.employee_id
                    JOIN auth.users au ON au.id = ue.auth_user_id
                    JOIN auth.user_roles ur ON ur.user_id = au.id
                    JOIN auth.roles ar ON ar.id = ur.role_id
                    WHERE e.status NOT IN ('DRAFT', 'REJECTED')
                      AND ue.is_active = true
                      AND au.enabled = true
                      AND UPPER(ar.name) = 'ROLE_EMPLOYEE'
                    GROUP BY e.employee_id
                    ORDER BY total_amount DESC
                    LIMIT ?
                    """, (rs, rowNum) -> new TopSpenderDto(
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("claim_count")
            ), sanitizedLimit);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute top spenders", e);
        }
    }

    private List<RecentExpenseDto> queryRecentExpenses(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new RecentExpenseDto(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getDouble("amount"),
                    rs.getString("status"),
                    rs.getString("risk_level"),
                    toLocalDateTime(rs.getTimestamp("created_at")),
                    rs.getString("merchant_name")
            ), args);
        } catch (Exception e) {
            throw analyticsFailure("Failed to fetch recent expenses", e);
        }
    }

    private List<MonthlySpendDto> queryMonthlyTrends(String sql, Object... args) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> new MonthlySpendDto(
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("expense_count")
            ), args);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute monthly trends", e);
        }
    }

    private List<CategorySpendDto> queryCategoryBreakdown(String totalSql, String detailSql, Object... args) {
        try {
            BigDecimal grandTotal = jdbcTemplate.queryForObject(totalSql, BigDecimal.class, args);
            if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
                return List.of();
            }

            return jdbcTemplate.query(detailSql, (rs, rowNum) -> {
                BigDecimal categoryTotal = rs.getBigDecimal("total_amount");
                double percentage = categoryTotal.divide(grandTotal, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                return new CategorySpendDto(
                        rs.getString("category_name"),
                        rs.getString("category_code"),
                        categoryTotal,
                        Math.round(percentage * 100.0) / 100.0,
                        rs.getLong("expense_count")
                );
            }, args);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute category breakdown", e);
        }
    }

    private EmployeeReportDto queryEmployeeReport(Long employeeAuthUserId) {
        try {
            List<EmployeeReportDto> results = jdbcTemplate.query("""
                    SELECT
                        e.employee_id,
                        COALESCE(SUM(e.amount), 0)                                              AS total_amount,
                        COUNT(e.id)                                                              AS total_claims,
                        COALESCE(SUM(CASE WHEN e.status IN ('APPROVED','REIMBURSED')
                                         THEN e.amount ELSE 0 END), 0)                         AS approved_amount,
                        COALESCE(SUM(CASE WHEN e.status = 'REJECTED'
                                         THEN e.amount ELSE 0 END), 0)                         AS rejected_amount,
                        COALESCE(SUM(CASE WHEN e.status IN ('PENDING','SUBMITTED','UNDER_REVIEW')
                                         THEN e.amount ELSE 0 END), 0)                         AS pending_amount
                    FROM expense.expenses e
                    WHERE e.employee_id = ?
                    GROUP BY e.employee_id
                    """, (rs, rowNum) -> new EmployeeReportDto(
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("total_claims"),
                    rs.getBigDecimal("approved_amount"),
                    rs.getBigDecimal("rejected_amount"),
                    rs.getBigDecimal("pending_amount")
            ), employeeAuthUserId);

            if (results.isEmpty()) {
                return new EmployeeReportDto(employeeAuthUserId,
                        BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }

            return results.get(0);
        } catch (Exception e) {
            throw analyticsFailure("Failed to compute employee report", e);
        }
    }

    private Long getDepartmentIdForAuthUser(Long authUserId) {
        List<Long> results = jdbcTemplate.query("""
                SELECT department_id
                FROM users.employees
                WHERE auth_user_id = ?
                """, (rs, rowNum) -> (Long) rs.getObject("department_id"), authUserId);

        if (results.isEmpty() || results.get(0) == null) {
            throw new ResourceNotFoundException("No employee department found for auth user id: " + authUserId);
        }

        return results.get(0);
    }

    private void ensureSameDepartment(Long requesterAuthUserId, Long employeeAuthUserId) {
        Long requesterDepartmentId = getDepartmentIdForAuthUser(requesterAuthUserId);
        Long employeeDepartmentId = getDepartmentIdForAuthUser(employeeAuthUserId);

        if (!requesterDepartmentId.equals(employeeDepartmentId)) {
            throw new AccessDeniedException("Managers can only access employees within their department");
        }
    }

    private void ensureEmployeeRole(Long authUserId) {
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

        if (results.isEmpty() || !Boolean.TRUE.equals(results.get(0))) {
            throw new AccessDeniedException("Managers can only access employee accounts");
        }
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0 || limit > 100) {
            return 10;
        }
        return limit;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
    }

    private AnalyticsException analyticsFailure(String message, Exception cause) {
        log.error("{}: {}", message, cause.getMessage(), cause);
        return new AnalyticsException(message, cause);
    }
}
