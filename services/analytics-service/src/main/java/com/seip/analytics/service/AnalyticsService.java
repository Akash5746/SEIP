package com.seip.analytics.service;

import com.seip.analytics.dto.*;
import com.seip.analytics.exception.AnalyticsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    // -----------------------------------------------------------------------
    // Organisation-wide summary
    // -----------------------------------------------------------------------
    public OrgSummaryDto getOrgSummary() {
        log.debug("Running org summary query");
        try {
            String sql = """
                SELECT
                    COUNT(*)                                                          AS total_expenses,
                    COALESCE(SUM(e.amount), 0)                                       AS total_amount,
                    COALESCE(SUM(CASE WHEN e.status IN ('PENDING','SUBMITTED','UNDER_REVIEW')
                                     THEN e.amount ELSE 0 END), 0)                  AS pending_amount,
                    COALESCE(SUM(CASE WHEN e.status IN ('APPROVED','REIMBURSED')
                                     THEN e.amount ELSE 0 END), 0)                  AS approved_amount,
                    COALESCE(SUM(CASE WHEN e.status = 'REJECTED'
                                     THEN e.amount ELSE 0 END), 0)                  AS rejected_amount,
                    COALESCE(AVG(fa.risk_score), 0)                                 AS avg_risk_score,
                    COUNT(CASE WHEN fa.risk_level = 'HIGH' THEN 1 END)              AS high_risk_count
                FROM expense_schema.expenses e
                LEFT JOIN fraud_schema.fraud_analysis fa ON fa.expense_id = e.id
                """;

            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new OrgSummaryDto(
                    rs.getLong("total_expenses"),
                    rs.getBigDecimal("total_amount"),
                    rs.getBigDecimal("pending_amount"),
                    rs.getBigDecimal("approved_amount"),
                    rs.getBigDecimal("rejected_amount"),
                    rs.getDouble("avg_risk_score"),
                    rs.getLong("high_risk_count")
            ));
        } catch (Exception e) {
            log.error("Error computing org summary: {}", e.getMessage(), e);
            throw new AnalyticsException("Failed to compute organisation summary", e);
        }
    }

    // -----------------------------------------------------------------------
    // Monthly spend trends for a given year
    // -----------------------------------------------------------------------
    public List<MonthlySpendDto> getMonthlyTrends(int year) {
        log.debug("Running monthly trends query for year={}", year);
        try {
            String sql = """
                SELECT
                    EXTRACT(MONTH FROM e.expense_date)::INT AS month,
                    EXTRACT(YEAR  FROM e.expense_date)::INT AS year,
                    COALESCE(SUM(e.amount), 0)              AS total_amount,
                    COUNT(*)                                AS expense_count
                FROM expense_schema.expenses e
                WHERE EXTRACT(YEAR FROM e.expense_date) = ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY month, year
                ORDER BY month ASC
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> new MonthlySpendDto(
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("expense_count")
            ), year);
        } catch (Exception e) {
            log.error("Error computing monthly trends for year={}: {}", year, e.getMessage(), e);
            throw new AnalyticsException("Failed to compute monthly trends for year " + year, e);
        }
    }

    // -----------------------------------------------------------------------
    // Category breakdown for a date range
    // -----------------------------------------------------------------------
    public List<CategorySpendDto> getCategoryBreakdown(LocalDate from, LocalDate to) {
        log.debug("Running category breakdown query from={} to={}", from, to);
        try {
            // First compute the grand total for percentage calculation
            String totalSql = """
                SELECT COALESCE(SUM(e.amount), 0)
                FROM expense_schema.expenses e
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                """;
            BigDecimal grandTotal = jdbcTemplate.queryForObject(
                    totalSql, BigDecimal.class, from, to);
            if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
                return List.of();
            }

            String sql = """
                SELECT
                    ec.name                              AS category_name,
                    ec.code                              AS category_code,
                    COALESCE(SUM(e.amount), 0)           AS total_amount,
                    COUNT(e.id)                          AS expense_count
                FROM expense_schema.expenses e
                JOIN expense_schema.expense_categories ec ON e.category_id = ec.id
                WHERE e.expense_date BETWEEN ? AND ?
                  AND e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY ec.name, ec.code
                ORDER BY total_amount DESC
                """;

            BigDecimal finalGrandTotal = grandTotal;
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                BigDecimal catTotal = rs.getBigDecimal("total_amount");
                double pct = catTotal.divide(finalGrandTotal, 6, RoundingMode.HALF_UP)
                                     .multiply(BigDecimal.valueOf(100))
                                     .doubleValue();
                return new CategorySpendDto(
                        rs.getString("category_name"),
                        rs.getString("category_code"),
                        catTotal,
                        Math.round(pct * 100.0) / 100.0,
                        rs.getLong("expense_count")
                );
            }, from, to);
        } catch (Exception e) {
            log.error("Error computing category breakdown: {}", e.getMessage(), e);
            throw new AnalyticsException("Failed to compute category breakdown", e);
        }
    }

    // -----------------------------------------------------------------------
    // Employee report
    // -----------------------------------------------------------------------
    public EmployeeReportDto getEmployeeReport(Long employeeId) {
        log.debug("Running employee report for employeeId={}", employeeId);
        try {
            String sql = """
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
                FROM expense_schema.expenses e
                WHERE e.employee_id = ?
                GROUP BY e.employee_id
                """;

            List<EmployeeReportDto> results = jdbcTemplate.query(sql, (rs, rowNum) -> new EmployeeReportDto(
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("total_claims"),
                    rs.getBigDecimal("approved_amount"),
                    rs.getBigDecimal("rejected_amount"),
                    rs.getBigDecimal("pending_amount")
            ), employeeId);

            if (results.isEmpty()) {
                return new EmployeeReportDto(employeeId,
                        BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            return results.get(0);
        } catch (Exception e) {
            log.error("Error computing employee report for employeeId={}: {}", employeeId, e.getMessage(), e);
            throw new AnalyticsException("Failed to compute employee report for id: " + employeeId, e);
        }
    }

    // -----------------------------------------------------------------------
    // Department breakdown (joining user_schema for dept info)
    // -----------------------------------------------------------------------
    public List<DepartmentReportDto> getDepartmentReport() {
        log.debug("Running department report query");
        try {
            String sql = """
                SELECT
                    u.department_id,
                    COALESCE(d.name, 'Dept-' || u.department_id::TEXT) AS department_name,
                    COALESCE(SUM(e.amount), 0)                         AS total_amount,
                    COUNT(DISTINCT e.employee_id)                      AS employee_count,
                    CASE WHEN COUNT(DISTINCT e.employee_id) > 0
                         THEN COALESCE(SUM(e.amount), 0)
                              / COUNT(DISTINCT e.employee_id)
                         ELSE 0
                    END                                                 AS avg_per_employee
                FROM expense_schema.expenses e
                JOIN user_schema.users u ON u.id = e.employee_id
                LEFT JOIN user_schema.departments d ON d.id = u.department_id
                WHERE e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY u.department_id, d.name
                ORDER BY total_amount DESC
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> new DepartmentReportDto(
                    rs.getLong("department_id"),
                    rs.getString("department_name"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("employee_count"),
                    rs.getBigDecimal("avg_per_employee")
            ));
        } catch (Exception e) {
            log.error("Error computing department report: {}", e.getMessage(), e);
            throw new AnalyticsException("Failed to compute department report", e);
        }
    }

    // -----------------------------------------------------------------------
    // Fraud trends for a given year
    // -----------------------------------------------------------------------
    public List<FraudTrendDto> getFraudTrends(int year) {
        log.debug("Running fraud trends query for year={}", year);
        try {
            String sql = """
                SELECT
                    EXTRACT(MONTH FROM fa.analysis_time)::INT   AS month,
                    EXTRACT(YEAR  FROM fa.analysis_time)::INT   AS year,
                    COUNT(*)                                     AS total_analyzed,
                    COUNT(CASE WHEN fa.risk_level = 'HIGH'   THEN 1 END) AS high_risk,
                    COUNT(CASE WHEN fa.risk_level = 'MEDIUM' THEN 1 END) AS medium_risk,
                    COUNT(CASE WHEN fa.risk_level = 'LOW'    THEN 1 END) AS low_risk
                FROM fraud_schema.fraud_analysis fa
                WHERE EXTRACT(YEAR FROM fa.analysis_time) = ?
                GROUP BY month, year
                ORDER BY month ASC
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> new FraudTrendDto(
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getLong("total_analyzed"),
                    rs.getLong("high_risk"),
                    rs.getLong("medium_risk"),
                    rs.getLong("low_risk")
            ), year);
        } catch (Exception e) {
            log.error("Error computing fraud trends for year={}: {}", year, e.getMessage(), e);
            throw new AnalyticsException("Failed to compute fraud trends for year " + year, e);
        }
    }

    // -----------------------------------------------------------------------
    // Top spenders
    // -----------------------------------------------------------------------
    public List<TopSpenderDto> getTopSpenders(int limit) {
        log.debug("Running top spenders query, limit={}", limit);
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }
        try {
            String sql = """
                SELECT
                    e.employee_id,
                    COALESCE(SUM(e.amount), 0) AS total_amount,
                    COUNT(e.id)                AS claim_count
                FROM expense_schema.expenses e
                WHERE e.status NOT IN ('DRAFT', 'REJECTED')
                GROUP BY e.employee_id
                ORDER BY total_amount DESC
                LIMIT ?
                """;

            int finalLimit = limit;
            return jdbcTemplate.query(sql, (rs, rowNum) -> new TopSpenderDto(
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("claim_count")
            ), finalLimit);
        } catch (Exception e) {
            log.error("Error computing top spenders: {}", e.getMessage(), e);
            throw new AnalyticsException("Failed to compute top spenders", e);
        }
    }
}
