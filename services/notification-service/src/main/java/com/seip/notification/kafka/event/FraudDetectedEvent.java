package com.seip.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
