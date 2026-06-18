package com.seip.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSubmittedEvent {
    private Long expenseId;
    private String expenseNumber;
    private Long employeeId;
    private BigDecimal amount;
    private String title;
    private LocalDateTime submittedAt;
}
