package com.seip.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body received from ML service for fraud probability prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictResponse {

    private Double fraudProbability;
    private String modelVersion;
    private String predictionLabel;
}
