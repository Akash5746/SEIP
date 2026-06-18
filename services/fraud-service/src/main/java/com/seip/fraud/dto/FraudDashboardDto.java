package com.seip.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard summary DTO aggregating fraud statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDashboardDto {

    private long totalAnalyzed;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;
    private long duplicatesDetected;
    private BigDecimal fraudRatePercent;
}
