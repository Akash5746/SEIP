package com.seip.expense.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewExpenseRequest {

    @NotNull(message = "Review decision is required")
    private ReviewDecision decision;

    private String notes;

    public enum ReviewDecision {
        APPROVED, REJECTED
    }
}
