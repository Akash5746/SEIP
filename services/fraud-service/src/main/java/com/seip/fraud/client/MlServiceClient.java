package com.seip.fraud.client;

import com.seip.fraud.dto.MlPredictRequest;
import com.seip.fraud.dto.MlPredictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Reactive client for the ML fraud prediction micro-service.
 * If the ML service is unavailable, the fraud analysis still proceeds —
 * only rule-based flags are used in that scenario.
 */
@Slf4j
@Service
public class MlServiceClient {

    private final WebClient mlServiceWebClient;

    public MlServiceClient(@Qualifier("mlServiceWebClient") WebClient mlServiceWebClient) {
        this.mlServiceWebClient = mlServiceWebClient;
    }

    /**
     * Request a fraud probability score from the ML model.
     *
     * @param amount          expense amount
     * @param categoryCode    expense category code
     * @param monthlyCount    employee's monthly claim count
     * @return Mono<Double> probability [0.0, 1.0], or 0.0 on error
     */
    public Mono<Double> predictFraud(BigDecimal amount, String categoryCode, int monthlyCount) {
        MlPredictRequest request = MlPredictRequest.builder()
                .amount(amount)
                .categoryCode(categoryCode)
                .monthlyClaimCount(monthlyCount)
                .build();

        return mlServiceWebClient.post()
                .uri("/ml/predict-fraud")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MlPredictResponse.class)
                .map(response -> {
                    Double prob = response.getFraudProbability();
                    log.debug("ML fraud probability: {}, label: {}", prob, response.getPredictionLabel());
                    return prob != null ? prob : 0.0;
                })
                .onErrorResume(ex -> {
                    log.warn("ML service unavailable – skipping ML prediction: {}", ex.getMessage());
                    return Mono.just(0.0);
                });
    }
}
