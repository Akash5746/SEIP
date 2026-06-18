package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rule: Flags expenses where the amount exceeds ₹50,000.
 * Risk score contribution: 30
 */
@Component
public class HighAmountRule extends FraudDetectionRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("50000");

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (context.getAmount() != null && context.getAmount().compareTo(THRESHOLD) > 0) {
            return new RuleResult(
                    30,
                    FlagType.HIGH_AMOUNT,
                    "Amount ₹" + context.getAmount() + " exceeds ₹50,000 threshold"
            );
        }
        return RuleResult.notTriggered();
    }
}
