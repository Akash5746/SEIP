package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates all fraud detection rules against a given context.
 * Accumulates risk scores from triggered rules and determines the overall risk level.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudRuleEngine {

    private final List<FraudDetectionRule> rules;

    /**
     * Run all registered fraud detection rules against the provided context.
     *
     * @param context enriched fraud check context
     * @return aggregated FraudAnalysisResult with total score, level, and triggered rules
     */
    public FraudAnalysisResult runAnalysis(FraudCheckContext context) {
        log.info("Starting fraud rule analysis for expenseId={}, employeeId={}",
                context.getExpenseId(), context.getEmployeeId());

        List<RuleResult> triggeredRules = new ArrayList<>();
        int totalScore = 0;
        boolean isDuplicate = false;

        for (FraudDetectionRule rule : rules) {
            try {
                RuleResult result = rule.evaluate(context);
                if (result.isTriggered()) {
                    triggeredRules.add(result);
                    totalScore += result.getRiskScore();
                    if (result.getFlagType() == FlagType.DUPLICATE_CLAIM) {
                        isDuplicate = true;
                    }
                    log.debug("Rule triggered: flagType={}, score={}, description={}",
                            result.getFlagType(), result.getRiskScore(), result.getDescription());
                }
            } catch (Exception ex) {
                log.error("Error evaluating rule {}: {}", rule.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }

        // Cap the total score at 100
        totalScore = Math.min(totalScore, 100);

        // Determine risk level
        RiskLevel level;
        if (totalScore <= 30) {
            level = RiskLevel.LOW;
        } else if (totalScore <= 70) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.HIGH;
        }

        log.info("Fraud analysis complete: expenseId={}, totalScore={}, riskLevel={}, triggeredRules={}",
                context.getExpenseId(), totalScore, level, triggeredRules.size());

        return new FraudAnalysisResult(context.getExpenseId(), totalScore, level, isDuplicate, triggeredRules);
    }
}
