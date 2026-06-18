package com.seip.expense.kafka;

import com.seip.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAnalysisResponseConsumer {

    private final ExpenseService expenseService;

    @KafkaListener(
            topics = "fraud.analysis.response",
            groupId = "expense-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onFraudAnalysisResponse(
            @Payload FraudAnalysisResponseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received FraudAnalysisResponseEvent from topic={} partition={} offset={}: expenseId={}, score={}, level={}",
                topic, partition, offset,
                event.getExpenseId(), event.getRiskScore(), event.getRiskLevel());

        try {
            expenseService.updateRiskScore(
                    event.getExpenseId(),
                    event.getRiskScore(),
                    event.getRiskLevel());
            log.info("Updated risk score for expense {}: {}/{}",
                    event.getExpenseId(), event.getRiskScore(), event.getRiskLevel());
        } catch (Exception e) {
            log.error("Failed to update risk score for expense {}: {}",
                    event.getExpenseId(), e.getMessage(), e);
            // In production, this should be sent to a DLQ
        }
    }
}
