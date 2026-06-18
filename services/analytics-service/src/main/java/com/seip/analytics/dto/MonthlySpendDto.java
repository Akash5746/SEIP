package com.seip.analytics.dto;

import java.math.BigDecimal;

public record MonthlySpendDto(
        int month,
        int year,
        BigDecimal totalAmount,
        long expenseCount
) {}
