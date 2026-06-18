package com.seip.expense.dto;

import com.seip.expense.entity.ExpenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitExpenseResponse {

    private String expenseNumber;
    private ExpenseStatus status;
    private String message;
}
