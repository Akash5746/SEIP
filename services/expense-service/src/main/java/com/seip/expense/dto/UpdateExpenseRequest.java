package com.seip.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpenseRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private LocalDate expenseDate;

    private Long categoryId;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 200, message = "Merchant name must not exceed 200 characters")
    private String merchantName;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @Valid
    private List<ExpenseItemRequest> items;
}
