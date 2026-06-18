package com.seip.fraud.rules;

/**
 * Abstract base class for all fraud detection rules (Chain of Responsibility pattern).
 * Each rule evaluates the context independently and returns a RuleResult.
 */
public abstract class FraudDetectionRule {

    /**
     * Evaluate the fraud check context and return a result indicating
     * whether this rule was triggered and its risk contribution.
     *
     * @param context the enriched fraud check context
     * @return RuleResult with triggered flag, score, and description
     */
    public abstract RuleResult evaluate(FraudCheckContext context);
}
