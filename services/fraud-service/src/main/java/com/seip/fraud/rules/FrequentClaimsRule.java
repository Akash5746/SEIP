package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

/**
 * Rule: Flags employees who have submitted more than 5 expense claims in the current month.
 * Risk score contribution: 20
 */
@Component
public class FrequentClaimsRule extends FraudDetectionRule {

    private static final int MONTHLY_CLAIM_THRESHOLD = 5;

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (context.getMonthlyClaimCount() > MONTHLY_CLAIM_THRESHOLD) {
            return new RuleResult(
                    20,
                    FlagType.FREQUENT_CLAIMS,
                    "Employee has submitted " + context.getMonthlyClaimCount()
                            + " claims this month, exceeding the threshold of " + MONTHLY_CLAIM_THRESHOLD
            );
        }
        return RuleResult.notTriggered();
    }
}
