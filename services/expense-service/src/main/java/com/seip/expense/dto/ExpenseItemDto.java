package com.seip.expense.dto;

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
public class ExpenseItemDto {

    private Long id;
    private String description;
    private BigDecimal amount;
    private Integer quantity;
    private LocalDateTime createdAt;
}
