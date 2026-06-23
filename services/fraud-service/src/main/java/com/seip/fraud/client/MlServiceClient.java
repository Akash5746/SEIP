package com.seip.fraud.client;

import com.seip.fraud.dto.FraudCheckRequestEvent;
import com.seip.fraud.dto.MlPredictRequest;
import com.seip.fraud.dto.MlPredictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;

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
     * @param event           expense event under review
     * @param monthlyCount    employee's monthly claim count
     * @return Mono<Double> probability [0.0, 1.0], or 0.0 on error
     */
    public Mono<Double> predictFraud(FraudCheckRequestEvent event, int monthlyCount) {
        int dayOfWeek = event.getExpenseDate() != null ? event.getExpenseDate().getDayOfWeek().getValue() - 1 : 0;
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY.getValue() - 1
                || dayOfWeek == DayOfWeek.SUNDAY.getValue() - 1;

        MlPredictRequest request = MlPredictRequest.builder()
                .expenseId(event.getExpenseId())
                .employeeId(event.getEmployeeId())
                .amount(event.getAmount())
                .categoryCode(event.getCategoryCode())
                .monthlyClaimCount(monthlyCount)
                .frequencyScore(Math.min(monthlyCount / 10.0, 1.0))
                .employeeHistoryScore(0.5)
                .dayOfWeek(dayOfWeek)
                .weekend(isWeekend)
                .merchantName(event.getMerchantName())
                .build();

        return mlServiceWebClient.post()
                .uri("/ml/predict-fraud")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MlPredictResponse.class)
                .map(response -> {
                    Double prob = response.getFraudProbability();
                    log.debug("ML fraud probability: {}, label: {}, model: {}",
                            prob, response.getPredictionLabel(), response.getModelVersion());
                    return prob != null ? prob : 0.0;
                })
                .onErrorResume(ex -> {
                    log.warn("ML service unavailable – skipping ML prediction: {}", ex.getMessage());
                    return Mono.just(0.0);
                });
    }
}
