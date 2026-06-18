package com.seip.expense.dto;

import com.seip.expense.entity.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {

    private Long id;
    private String expenseNumber;
    private Long employeeId;
    private ExpenseCategoryDto category;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private LocalDate expenseDate;
    private ExpenseStatus status;
    private Integer riskScore;
    private String riskLevel;
    private Long reviewerId;
    private String reviewNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime reimbursedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExpenseItemDto> items;
    private List<ReceiptDto> receipts;
}
