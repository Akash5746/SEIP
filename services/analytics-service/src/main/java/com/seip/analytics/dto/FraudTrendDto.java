package com.seip.analytics.dto;

public record FraudTrendDto(
        int month,
        int year,
        long totalAnalyzed,
        long highRisk,
        long mediumRisk,
        long lowRisk
) {}
