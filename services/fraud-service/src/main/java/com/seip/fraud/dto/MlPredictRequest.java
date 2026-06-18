package com.seip.fraud.dto;

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

    private BigDecimal amount;
    private String categoryCode;
    private int monthlyClaimCount;
}
