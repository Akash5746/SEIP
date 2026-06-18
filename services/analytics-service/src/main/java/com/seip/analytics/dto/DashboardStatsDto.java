package com.seip.analytics.dto;

public record DashboardStatsDto(
        long totalExpenses,
        long pendingCount,
        double thisMonthAmount,
        long highRiskAlerts,
        long approvedThisMonth,
        long rejectedThisMonth
) {}
