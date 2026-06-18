package com.seip.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seip.notification.entity.NotificationType;
import com.seip.notification.kafka.event.ExpenseApprovedEvent;
import com.seip.notification.kafka.event.ExpenseRejectedEvent;
import com.seip.notification.kafka.event.ExpenseSubmittedEvent;
import com.seip.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    // Hardcoded email addresses (replace with service lookups in production)
    private static final String MANAGER_EMAIL = "manager@seip.com";
    private static final String MANAGER_NAME  = "Expense Manager";
    private static final String EMPLOYEE_EMAIL = "employee@seip.com";
    private static final String EMPLOYEE_NAME  = "Employee";

    @KafkaListener(topics = "expense.submitted", groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleExpenseSubmitted(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received expense.submitted event from topic: {}", topic);
        try {
            ExpenseSubmittedEvent event = objectMapper.readValue(payload, ExpenseSubmittedEvent.class);

            Map<String, Object> vars = new HashMap<>();
            vars.put("expenseTitle", event.getTitle());
            vars.put("amount", event.getAmount().toPlainString());
            vars.put("employeeName", "Employee #" + event.getEmployeeId());
            vars.put("expenseNumber", event.getExpenseNumber());
            vars.put("submittedAt", event.getSubmittedAt() != null ? event.getSubmittedAt().toString() : "N/A");

            emailService.sendHtmlEmail(
                    MANAGER_EMAIL,
                    MANAGER_NAME,
                    "New Expense Submitted: " + event.getTitle(),
                    "expense-submitted",
                    vars,
                    NotificationType.EXPENSE_SUBMITTED,
                    String.valueOf(event.getExpenseId())
            );
        } catch (Exception e) {
            log.error("Error processing expense.submitted event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "expense.approved", groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleExpenseApproved(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received expense.approved event from topic: {}", topic);
        try {
            ExpenseApprovedEvent event = objectMapper.readValue(payload, ExpenseApprovedEvent.class);

            Map<String, Object> vars = new HashMap<>();
            vars.put("expenseId", event.getExpenseId());
            vars.put("amount", event.getAmount().toPlainString());
            vars.put("reviewerId", event.getReviewerId());
            vars.put("notes", event.getNotes() != null ? event.getNotes() : "No additional notes.");

            emailService.sendHtmlEmail(
                    EMPLOYEE_EMAIL,
                    EMPLOYEE_NAME,
                    "Your Expense Has Been Approved ✓",
                    "expense-approved",
                    vars,
                    NotificationType.EXPENSE_APPROVED,
                    String.valueOf(event.getExpenseId())
            );
        } catch (Exception e) {
            log.error("Error processing expense.approved event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "expense.rejected", groupId = "notification-service",
                   containerFactory = "kafkaListenerContainerFactory")
    public void handleExpenseRejected(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("Received expense.rejected event from topic: {}", topic);
        try {
            ExpenseRejectedEvent event = objectMapper.readValue(payload, ExpenseRejectedEvent.class);

            Map<String, Object> vars = new HashMap<>();
            vars.put("expenseId", event.getExpenseId());
            vars.put("amount", event.getAmount().toPlainString());
            vars.put("reviewerId", event.getReviewerId());
            vars.put("rejectionReason", event.getNotes() != null ? event.getNotes() : "No reason provided.");

            emailService.sendHtmlEmail(
                    EMPLOYEE_EMAIL,
                    EMPLOYEE_NAME,
                    "Your Expense Has Been Rejected",
                    "expense-rejected",
                    vars,
                    NotificationType.EXPENSE_REJECTED,
                    String.valueOf(event.getExpenseId())
            );
        } catch (Exception e) {
            log.error("Error processing expense.rejected event: {}", e.getMessage(), e);
        }
    }
}
