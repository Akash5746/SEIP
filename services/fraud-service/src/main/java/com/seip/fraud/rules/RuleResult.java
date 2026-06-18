package com.seip.fraud.rules;

import com.seip.fraud.entity.FlagType;

/**
 * Immutable result returned by each fraud detection rule evaluation.
 */
public final class RuleResult {

    private final int riskScore;
    private final FlagType flagType;
    private final String description;
    private final boolean triggered;

    /** Triggered rule result */
    public RuleResult(int riskScore, FlagType flagType, String description) {
        this.riskScore = riskScore;
        this.flagType = flagType;
        this.description = description;
        this.triggered = true;
    }

    /** Non-triggered rule result */
    private RuleResult() {
        this.riskScore = 0;
        this.flagType = null;
        this.description = null;
        this.triggered = false;
    }

    public static RuleResult notTriggered() {
        return new RuleResult();
    }

    public int getRiskScore() {
        return riskScore;
    }

    public FlagType getFlagType() {
        return flagType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTriggered() {
        return triggered;
    }

    @Override
    public String toString() {
        return "RuleResult{triggered=" + triggered +
                ", flagType=" + flagType +
                ", riskScore=" + riskScore +
                ", description='" + description + "'}";
    }
}
