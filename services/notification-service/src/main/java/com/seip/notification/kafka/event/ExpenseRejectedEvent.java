package com.seip.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRejectedEvent {
    private Long expenseId;
    private Long employeeId;
    private Long reviewerId;
    private BigDecimal amount;
    private String notes;
}
