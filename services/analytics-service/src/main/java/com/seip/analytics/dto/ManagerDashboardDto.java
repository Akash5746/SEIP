package com.seip.analytics.dto;

import java.math.BigDecimal;

public record ManagerDashboardDto(
        String departmentName,
        long totalEmployees,
        long pendingApprovals,
        BigDecimal departmentExpensesThisMonth,
        long highRiskAlerts,
        long personalExpenseCount,
        BigDecimal personalThisMonthAmount
) {}
