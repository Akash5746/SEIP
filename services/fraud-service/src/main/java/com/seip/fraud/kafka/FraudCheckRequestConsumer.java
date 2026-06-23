package com.seip.fraud.kafka;

import com.seip.fraud.dto.FraudCheckRequestEvent;
import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.entity.RiskLevel;
import com.seip.fraud.service.FraudAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudCheckRequestConsumer {

    private final FraudAnalysisService fraudAnalysisService;
    private final FraudEventPublisher  fraudEventPublisher;

    /**
     * Consumes expense fraud-check requests from Kafka, runs the full analysis pipeline,
     * then publishes the result event. HIGH risk results trigger an additional alert event.
     */
    @KafkaListener(
            topics   = {"expense.fraud.check.request", "fraud.check.request"},
            groupId  = "fraud-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFraudCheckRequest(
            @Payload FraudCheckRequestEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received fraud check request: expenseId={}, topic={}, partition={}, offset={}",
                event.getExpenseId(), topic, partition, offset);

        try {
            // Run full analysis and persist
            FraudAnalysis analysis = fraudAnalysisService.analyzeExpense(event);

            // Always publish analysis response
            fraudEventPublisher.publishAnalysisResponse(
                    analysis.getExpenseId(),
                    analysis.getRiskScore(),
                    analysis.getRiskLevel().name()
            );

            // Publish a dedicated alert for HIGH risk cases
            if (RiskLevel.HIGH == analysis.getRiskLevel()) {
                fraudEventPublisher.publishFraudDetected(analysis);
            }

            log.info("Fraud analysis completed and events published for expenseId={}, riskLevel={}",
                    analysis.getExpenseId(), analysis.getRiskLevel());

        } catch (Exception ex) {
            log.error("Error processing fraud check request for expenseId={}: {}",
                    event.getExpenseId(), ex.getMessage(), ex);
            // Do NOT re-throw — message is acknowledged to avoid infinite retry loops.
            // A dead-letter topic strategy should be configured in KafkaConfig if needed.
        }
    }
}
