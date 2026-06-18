package com.seip.expense.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResponseEvent {

    private Long expenseId;
    private Integer riskScore;
    private String riskLevel;
}
