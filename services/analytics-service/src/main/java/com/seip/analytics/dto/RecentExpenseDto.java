package com.seip.analytics.dto;

import java.time.LocalDateTime;

public record RecentExpenseDto(
        long id,
        String title,
        double amount,
        String status,
        String riskLevel,
        LocalDateTime createdAt,
        String merchantName
) {}
