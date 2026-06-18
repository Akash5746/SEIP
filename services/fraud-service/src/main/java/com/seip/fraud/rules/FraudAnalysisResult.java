package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Aggregated result produced by FraudRuleEngine after running all rules.
 */
@Getter
@AllArgsConstructor
public class FraudAnalysisResult {

    private final Long expenseId;
    private final int totalRiskScore;
    private final RiskLevel riskLevel;
    private final boolean isDuplicate;
    private final List<RuleResult> triggeredRules;
}
