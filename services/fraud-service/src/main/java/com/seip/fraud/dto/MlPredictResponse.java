package com.seip.fraud.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("fraud_probability")
    private Double fraudProbability;

    @JsonProperty("model")
    private String modelVersion;

    @JsonProperty("risk_level")
    @JsonAlias("predictionLabel")
    private String predictionLabel;
}
