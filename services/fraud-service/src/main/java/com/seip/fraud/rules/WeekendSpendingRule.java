package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

/**
 * Rule: Flags expenses submitted on weekends (Saturday or Sunday).
 * Risk score contribution: 10
 */
@Component
public class WeekendSpendingRule extends FraudDetectionRule {

    @Override
    public RuleResult evaluate(FraudCheckContext context) {
        if (context.getExpenseDate() != null) {
            DayOfWeek day = context.getExpenseDate().getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return new RuleResult(
                        10,
                        FlagType.WEEKEND_SPENDING,
                        "Expense submitted on a weekend (" + day.name() + ")"
                );
            }
        }
        return RuleResult.notTriggered();
    }
}
