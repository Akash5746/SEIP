package com.seip.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seip.notification.entity.NotificationType;
import com.seip.notification.kafka.event.FraudDetectedEvent;
import com.seip.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAlertConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    private static final String ADMIN_EMAIL   = "admin@seip.com";
    private static final String ADMIN_NAME    = "SEIP Administrator";
    private static final String MANAGER_EMAIL = "manager@seip.com";
    private static final String MANAGER_NAME  = "Expense Manager";

    @KafkaListener(topics = "fraud.detected", groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleFraudDetected(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.warn("FRAUD ALERT received from topic: {}", topic);
        try {
            FraudDetectedEvent event = objectMapper.readValue(payload, FraudDetectedEvent.class);

            List<String> flags = event.getFlagTypes() != null ? event.getFlagTypes() : List.of();
            String flagsText = String.join(", ", flags);

            Map<String, Object> vars = new HashMap<>();
            vars.put("expenseId", event.getExpenseId());
            vars.put("employeeId", event.getEmployeeId());
            vars.put("riskScore", event.getRiskScore());
            vars.put("riskLevel", event.getRiskLevel());
            vars.put("flagTypes", flagsText);

            String subject = String.format("⚠️ FRAUD ALERT: Expense #%d — Risk Score %d (%s)",
                    event.getExpenseId(), event.getRiskScore(), event.getRiskLevel());

            String referenceId = String.valueOf(event.getExpenseId());

            // Send to admin
            emailService.sendHtmlEmail(
                    ADMIN_EMAIL,
                    ADMIN_NAME,
                    subject,
                    "fraud-alert",
                    vars,
                    NotificationType.FRAUD_ALERT,
                    referenceId
            );

            // Send to manager as well
            emailService.sendHtmlEmail(
                    MANAGER_EMAIL,
                    MANAGER_NAME,
                    subject,
                    "fraud-alert",
                    vars,
                    NotificationType.FRAUD_ALERT,
                    referenceId
            );

        } catch (Exception e) {
            log.error("Error processing fraud.detected event: {}", e.getMessage(), e);
        }
    }
}
