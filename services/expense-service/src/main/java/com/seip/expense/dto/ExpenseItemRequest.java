package com.seip.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseItemRequest {

    @NotBlank(message = "Item description is required")
    private String description;

    @NotNull(message = "Item amount is required")
    @DecimalMin(value = "0.01", message = "Item amount must be at least 0.01")
    private BigDecimal amount;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;
}
