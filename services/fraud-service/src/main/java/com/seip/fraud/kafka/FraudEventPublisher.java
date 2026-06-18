package com.seip.fraud.kafka;

import com.seip.fraud.dto.FraudAnalysisResponseEvent;
import com.seip.fraud.dto.FraudDetectedEvent;
import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.entity.FraudFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventPublisher {

    private static final String TOPIC_ANALYSIS_RESPONSE = "fraud.analysis.response";
    private static final String TOPIC_FRAUD_DETECTED     = "fraud.detected";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish the analysis result back to expense-service / notification-service.
     *
     * @param expenseId the analyzed expense
     * @param riskScore computed risk score [0–100]
     * @param riskLevel LOW / MEDIUM / HIGH
     */
    public void publishAnalysisResponse(Long expenseId, Integer riskScore, String riskLevel) {
        FraudAnalysisResponseEvent event = FraudAnalysisResponseEvent.builder()
                .expenseId(expenseId)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .build();

        kafkaTemplate.send(TOPIC_ANALYSIS_RESPONSE, String.valueOf(expenseId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud analysis response for expenseId={}: {}",
                                expenseId, ex.getMessage(), ex);
                    } else {
                        log.info("Published fraud analysis response: expenseId={}, riskLevel={}, topic={}",
                                expenseId, riskLevel, TOPIC_ANALYSIS_RESPONSE);
                    }
                });
    }

    /**
     * Publish a high-risk fraud alert so downstream services (notifications, audit) can react.
     *
     * @param analysis the persisted FraudAnalysis entity
     */
    public void publishFraudDetected(FraudAnalysis analysis) {
        List<String> flagTypes = analysis.getFlags().stream()
                .map(FraudFlag::getFlagType)
                .map(Enum::name)
                .collect(Collectors.toList());

        FraudDetectedEvent event = FraudDetectedEvent.builder()
                .expenseId(analysis.getExpenseId())
                .employeeId(analysis.getEmployeeId())
                .riskScore(analysis.getRiskScore())
                .riskLevel(analysis.getRiskLevel().name())
                .flagTypes(flagTypes)
                .build();

        kafkaTemplate.send(TOPIC_FRAUD_DETECTED, String.valueOf(analysis.getExpenseId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud-detected event for expenseId={}: {}",
                                analysis.getExpenseId(), ex.getMessage(), ex);
                    } else {
                        log.warn("HIGH RISK FRAUD DETECTED and published: expenseId={}, employeeId={}, flags={}",
                                analysis.getExpenseId(), analysis.getEmployeeId(), flagTypes);
                    }
                });
    }
}
