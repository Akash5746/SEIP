package com.seip.analytics.dto;

import java.math.BigDecimal;

public record CategorySpendDto(
        String categoryName,
        String categoryCode,
        BigDecimal totalAmount,
        double percentage,
        long expenseCount
) {}
