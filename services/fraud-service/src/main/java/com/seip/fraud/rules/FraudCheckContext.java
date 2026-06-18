package com.seip.fraud.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Context object carrying all data needed by fraud detection rules.
 * Populated by FraudAnalysisService before running the rule engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckContext {

    private Long expenseId;
    private Long employeeId;
    private BigDecimal amount;
    private String merchantName;
    private LocalDate expenseDate;
    private String categoryCode;
    private Long categoryId;

    /** Number of expense claims made by this employee in the current month */
    private int monthlyClaimCount;

    /** Whether an identical/similar claim already exists (fetched from expense-service) */
    private boolean isDuplicate;
}
