package com.seip.expense.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckRequestEvent {

    private Long expenseId;
    private Long employeeId;
    private BigDecimal amount;
    private String merchantName;
    private LocalDate expenseDate;
    private String categoryCode;
    private Long categoryId;
}
