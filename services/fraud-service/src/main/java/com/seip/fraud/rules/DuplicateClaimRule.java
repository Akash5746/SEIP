package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

/**
 * Rule: Flags expenses that are identified as duplicates of existing claims.
 * Risk score contribution: 40 (highest single rule weight)
 */
@Component
public class DuplicateClaimRule extends FraudDetectionRule {

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (context.isDuplicate()) {
            return new RuleResult(
                    40,
                    FlagType.DUPLICATE_CLAIM,
                    "Duplicate expense claim detected for employee " + context.getEmployeeId()
            );
        }
        return RuleResult.notTriggered();
    }
}
