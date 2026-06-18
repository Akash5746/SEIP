package com.seip.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published to topic: fraud.analysis.response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResponseEvent {

    private Long expenseId;
    private Integer riskScore;
    private String riskLevel;
}
