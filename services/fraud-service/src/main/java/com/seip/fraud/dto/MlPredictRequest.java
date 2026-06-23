package com.seip.fraud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body sent to ML service for fraud probability prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictRequest {

    @JsonProperty("expense_id")
    private Long expenseId;

    @JsonProperty("employee_id")
    private Long employeeId;

    private BigDecimal amount;

    @JsonProperty("category_code")
    private String categoryCode;

    @JsonProperty("monthly_claim_count")
    private int monthlyClaimCount;

    @JsonProperty("frequency_score")
    private double frequencyScore;

    @JsonProperty("employee_history_score")
    private double employeeHistoryScore;

    @JsonProperty("day_of_week")
    private int dayOfWeek;

    @JsonProperty("is_weekend")
    private boolean weekend;

    @JsonProperty("merchant_name")
    private String merchantName;
}
