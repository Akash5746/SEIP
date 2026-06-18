package com.seip.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single fraud flag on an analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlagDto {

    private Long id;
    private String flagType;
    private String flagDescription;
    private Integer riskContribution;
}
