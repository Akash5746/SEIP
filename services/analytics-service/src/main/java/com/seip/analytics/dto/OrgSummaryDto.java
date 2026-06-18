package com.seip.analytics.dto;

import java.math.BigDecimal;

public record OrgSummaryDto(
        long totalExpenses,
        BigDecimal totalAmount,
        BigDecimal pendingAmount,
        BigDecimal approvedAmount,
        BigDecimal rejectedAmount,
        double avgRiskScore,
        long highRiskCount
) {}
