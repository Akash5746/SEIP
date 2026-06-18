package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rule: Flags MISC category expenses exceeding ₹10,000.
 * Miscellaneous high-value claims are a common fraud vector.
 * Risk score contribution: 15
 */
@Component
public class SuspiciousCategoryRule extends FraudDetectionRule {

    private static final String SUSPICIOUS_CATEGORY = "MISC";
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("10000");

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (SUSPICIOUS_CATEGORY.equalsIgnoreCase(context.getCategoryCode())
                && context.getAmount() != null
                && context.getAmount().compareTo(AMOUNT_THRESHOLD) > 0) {
            return new RuleResult(
                    15,
                    FlagType.SUSPICIOUS_CATEGORY,
                    "MISC category expense of ₹" + context.getAmount()
                            + " exceeds ₹10,000 threshold – suspicious category claim"
            );
        }
        return RuleResult.notTriggered();
    }
}
