package com.seip.analytics.dto;

import java.math.BigDecimal;

public record EmployeeReportDto(
        Long employeeId,
        BigDecimal totalAmount,
        long totalClaims,
        BigDecimal approvedAmount,
        BigDecimal rejectedAmount,
        BigDecimal pendingAmount
) {}
