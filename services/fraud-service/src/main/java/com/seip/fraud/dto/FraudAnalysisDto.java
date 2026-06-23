package com.seip.fraud.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isDuplicate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime analysisTime;

    private BigDecimal mlFraudProbability;
    private String analystNotes;
    private List<FraudFlagDto> flags;

    @JsonProperty("isDuplicate")
    public boolean getIsDuplicate() {
        return isDuplicate;
    }

    @JsonProperty("isDuplicate")
    public void setIsDuplicate(boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }
}
