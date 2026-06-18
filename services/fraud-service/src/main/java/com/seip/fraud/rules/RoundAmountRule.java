package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Rule: Flags suspiciously round amounts (multiples of 1000) for expenses >= ₹5,000.
 * Round amounts are a statistical indicator of fabricated receipts.
 * Risk score contribution: 5
 */
@Component
public class RoundAmountRule extends FraudDetectionRule {

    private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("5000");
    private static final BigDecimal DIVISOR = BigDecimal.valueOf(1000);

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (context.getAmount() != null
                && context.getAmount().compareTo(MINIMUM_AMOUNT) >= 0
                && context.getAmount().remainder(DIVISOR).compareTo(BigDecimal.ZERO) == 0) {
            return new RuleResult(
                    5,
                    FlagType.ROUND_AMOUNT,
                    "Expense amount ₹" + context.getAmount()
                            + " is a suspiciously round number (exact multiple of 1000)"
            );
        }
        return RuleResult.notTriggered();
    }
}
