package com.seip.analytics.dto;

import java.math.BigDecimal;

public record TopSpenderDto(
        Long employeeId,
        BigDecimal totalAmount,
        long claimCount
) {}
