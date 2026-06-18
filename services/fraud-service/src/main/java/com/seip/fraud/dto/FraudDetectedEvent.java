package com.seip.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kafka event published to topic: fraud.detected (only for HIGH risk analyses)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectedEvent {

    private Long expenseId;
    private Long employeeId;
    private Integer riskScore;
    private String riskLevel;
    private List<String> flagTypes;
}
