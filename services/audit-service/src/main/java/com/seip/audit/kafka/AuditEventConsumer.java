package com.seip.audit.kafka;

import com.seip.audit.entity.AuditLog;
import com.seip.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogRepository auditLogRepository;

    @KafkaListener(
            topics = {"expense.submitted", "expense.approved", "expense.rejected", "fraud.detected"},
            groupId = "audit-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAuditEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Audit consumer received event from topic={}, partition={}, offset={}", topic, partition, offset);

        try {
            String action = deriveAction(topic);
            String serviceName = deriveServiceName(topic);
            String resourceType = deriveResourceType(topic);
            String resourceId = extractResourceId(payload);

            AuditLog auditLog = AuditLog.builder()
                    .eventId(UUID.randomUUID().toString())
                    .userId(null)          // enriched downstream if needed
                    .username(null)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(payload)
                    .ipAddress(null)
                    .userAgent(null)
                    .serviceName(serviceName)
                    .success(true)
                    .errorMessage(null)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Saved audit log for action={}, resourceId={}", action, resourceId);

        } catch (Exception e) {
            log.error("Failed to persist audit log for topic={}: {}", topic, e.getMessage(), e);

            // Attempt to save a failure audit entry
            try {
                AuditLog errorLog = AuditLog.builder()
                        .eventId(UUID.randomUUID().toString())
                        .action(deriveAction(topic))
                        .resourceType(deriveResourceType(topic))
                        .serviceName(deriveServiceName(topic))
                        .details(payload)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();
                auditLogRepository.save(errorLog);
            } catch (Exception saveEx) {
                log.error("Could not save error audit log: {}", saveEx.getMessage(), saveEx);
            }
        }
    }

    /**
     * Derives a standardised action name from the Kafka topic.
     * e.g. "expense.submitted" → "EXPENSE_SUBMITTED"
     */
    private String deriveAction(String topic) {
        return topic.toUpperCase().replace(".", "_");
    }

    /**
     * Derives the originating service name from the topic prefix.
     * e.g. "expense.submitted" → "expense-service"
     */
    private String deriveServiceName(String topic) {
        String prefix = topic.contains(".") ? topic.substring(0, topic.indexOf('.')) : topic;
        return prefix + "-service";
    }

    /**
     * Derives the resource type from the topic.
     */
    private String deriveResourceType(String topic) {
        String prefix = topic.contains(".") ? topic.substring(0, topic.indexOf('.')) : topic;
        return prefix.toUpperCase();
    }

    /**
     * Attempts to extract a resourceId (expenseId) from the raw JSON payload
     * without a full deserialization, to avoid coupling to event schema.
     */
    private String extractResourceId(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            // Simple regex-free extraction for "expenseId": <number>
            int idx = payload.indexOf("\"expenseId\"");
            if (idx == -1) idx = payload.indexOf("\"id\"");
            if (idx == -1) return null;

            int colon = payload.indexOf(":", idx);
            if (colon == -1) return null;

            int start = colon + 1;
            while (start < payload.length() && (payload.charAt(start) == ' ' || payload.charAt(start) == '"')) {
                start++;
            }
            int end = start;
            while (end < payload.length() && (Character.isDigit(payload.charAt(end)))) {
                end++;
            }
            if (end > start) {
                return payload.substring(start, end);
            }
        } catch (Exception e) {
            log.warn("Could not extract resourceId from payload: {}", e.getMessage());
        }
        return null;
    }
}
