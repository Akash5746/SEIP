package com.seip.fraud.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full fraud analysis DTO including all triggered flags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisDto {

    private Long id;
    private Long expenseId;
    private Long employeeId;
    private Integer riskScore;
    private String riskLevel;
    private boolean isDuplicate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime analysisTime;

    private BigDecimal mlFraudProbability;
    private String analystNotes;
    private List<FraudFlagDto> flags;
}
