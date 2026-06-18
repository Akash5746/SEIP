package com.seip.expense.dto;

import com.seip.expense.entity.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDto {

    private Long id;
    private String expenseNumber;
    private String title;
    private BigDecimal amount;
    private String currency;
    private ExpenseStatus status;
    private Integer riskScore;
    private String riskLevel;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
}
