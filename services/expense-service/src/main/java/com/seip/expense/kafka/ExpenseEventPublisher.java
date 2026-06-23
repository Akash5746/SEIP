package com.seip.expense.kafka;

import com.seip.expense.entity.Expense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseEventPublisher {

    private static final String TOPIC_EXPENSE_SUBMITTED = "expense.submitted";
    private static final String TOPIC_EXPENSE_APPROVED = "expense.approved";
    private static final String TOPIC_EXPENSE_REJECTED = "expense.rejected";
    private static final String TOPIC_FRAUD_CHECK_REQUEST = "expense.fraud.check.request";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishExpenseSubmitted(Expense expense) {
        ExpenseSubmittedEvent event = ExpenseSubmittedEvent.builder()
                .expenseId(expense.getId())
                .expenseNumber(expense.getExpenseNumber())
                .employeeId(expense.getEmployeeId())
                .amount(expense.getAmount())
                .title(expense.getTitle())
                .submittedAt(expense.getSubmittedAt())
                .build();

        kafkaTemplate.send(TOPIC_EXPENSE_SUBMITTED, expense.getExpenseNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ExpenseSubmittedEvent for {}: {}",
                                expense.getExpenseNumber(), ex.getMessage());
                    } else {
                        log.info("Published ExpenseSubmittedEvent for expense: {}",
                                expense.getExpenseNumber());
                    }
                });
    }

    @Async
    public void publishExpenseApproved(Expense expense, Long reviewerId, String notes) {
        ExpenseApprovedEvent event = ExpenseApprovedEvent.builder()
                .expenseId(expense.getId())
                .employeeId(expense.getEmployeeId())
                .reviewerId(reviewerId)
                .amount(expense.getAmount())
                .notes(notes)
                .build();

        kafkaTemplate.send(TOPIC_EXPENSE_APPROVED, expense.getExpenseNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ExpenseApprovedEvent for {}: {}",
                                expense.getExpenseNumber(), ex.getMessage());
                    } else {
                        log.info("Published ExpenseApprovedEvent for expense: {}",
                                expense.getExpenseNumber());
                    }
                });
    }

    @Async
    public void publishExpenseRejected(Expense expense, Long reviewerId, String notes) {
        ExpenseRejectedEvent event = ExpenseRejectedEvent.builder()
                .expenseId(expense.getId())
                .employeeId(expense.getEmployeeId())
                .reviewerId(reviewerId)
                .amount(expense.getAmount())
                .notes(notes)
                .build();

        kafkaTemplate.send(TOPIC_EXPENSE_REJECTED, expense.getExpenseNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ExpenseRejectedEvent for {}: {}",
                                expense.getExpenseNumber(), ex.getMessage());
                    } else {
                        log.info("Published ExpenseRejectedEvent for expense: {}",
                                expense.getExpenseNumber());
                    }
                });
    }

    @Async
    public void publishFraudCheckRequest(Expense expense) {
        FraudCheckRequestEvent event = FraudCheckRequestEvent.builder()
                .expenseId(expense.getId())
                .employeeId(expense.getEmployeeId())
                .amount(expense.getAmount())
                .merchantName(expense.getMerchantName())
                .expenseDate(expense.getExpenseDate())
                .categoryCode(expense.getCategory() != null ? expense.getCategory().getCode() : null)
                .categoryId(expense.getCategory() != null ? expense.getCategory().getId() : null)
                .build();

        kafkaTemplate.send(TOPIC_FRAUD_CHECK_REQUEST, expense.getExpenseNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish FraudCheckRequestEvent for {}: {}",
                                expense.getExpenseNumber(), ex.getMessage());
                    } else {
                        log.info("Published FraudCheckRequestEvent for expense: {}",
                                expense.getExpenseNumber());
                    }
                });
    }
}
