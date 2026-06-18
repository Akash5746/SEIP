package com.seip.fraud;

import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.RiskLevel;
import com.seip.fraud.rules.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudRuleEngine – unit tests")
class FraudRuleEngineTest {

    private FraudRuleEngine engine;

    // Individual rule instances (no Spring context needed)
    private HighAmountRule       highAmountRule;
    private WeekendSpendingRule  weekendSpendingRule;
    private DuplicateClaimRule   duplicateClaimRule;
    private FrequentClaimsRule   frequentClaimsRule;
    private SuspiciousCategoryRule suspiciousCategoryRule;
    private RoundAmountRule      roundAmountRule;

    @BeforeEach
    void setUp() {
        highAmountRule        = new HighAmountRule();
        weekendSpendingRule   = new WeekendSpendingRule();
        duplicateClaimRule    = new DuplicateClaimRule();
        frequentClaimsRule    = new FrequentClaimsRule();
        suspiciousCategoryRule = new SuspiciousCategoryRule();
        roundAmountRule       = new RoundAmountRule();

        engine = new FraudRuleEngine(List.of(
                highAmountRule, weekendSpendingRule, duplicateClaimRule,
                frequentClaimsRule, suspiciousCategoryRule, roundAmountRule));
    }

    // -----------------------------------------------------------------------
    // HighAmountRule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("HighAmountRule: amount above ₹50,000 → triggered")
    void testHighAmountRule_amountAboveThreshold_triggered() {
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("75000"))
                .build();

        RuleResult result = highAmountRule.evaluate(ctx);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getFlagType()).isEqualTo(FlagType.HIGH_AMOUNT);
        assertThat(result.getRiskScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("HighAmountRule: amount exactly ₹50,000 → NOT triggered (not strictly greater)")
    void testHighAmountRule_amountBelowThreshold_notTriggered() {
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("50000"))
                .build();

        RuleResult result = highAmountRule.evaluate(ctx);

        assertThat(result.isTriggered()).isFalse();
    }

    // -----------------------------------------------------------------------
    // WeekendSpendingRule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("WeekendSpendingRule: Saturday → triggered")
    void testWeekendSpendingRule_saturday_triggered() {
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        FraudCheckContext ctx = buildBaseContext()
                .expenseDate(saturday)
                .build();

        RuleResult result = weekendSpendingRule.evaluate(ctx);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getFlagType()).isEqualTo(FlagType.WEEKEND_SPENDING);
        assertThat(result.getRiskScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("WeekendSpendingRule: Monday → NOT triggered")
    void testWeekendSpendingRule_weekday_notTriggered() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        FraudCheckContext ctx = buildBaseContext()
                .expenseDate(monday)
                .build();

        RuleResult result = weekendSpendingRule.evaluate(ctx);

        assertThat(result.isTriggered()).isFalse();
    }

    // -----------------------------------------------------------------------
    // RoundAmountRule
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("RoundAmountRule: ₹10,000 (exact multiple of 1000, >= 5000) → triggered")
    void testRoundAmountRule_roundAmount_triggered() {
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("10000"))
                .build();

        RuleResult result = roundAmountRule.evaluate(ctx);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getFlagType()).isEqualTo(FlagType.ROUND_AMOUNT);
        assertThat(result.getRiskScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("RoundAmountRule: ₹10,500 (not a round multiple of 1000) → NOT triggered")
    void testRoundAmountRule_oddAmount_notTriggered() {
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("10500"))
                .build();

        RuleResult result = roundAmountRule.evaluate(ctx);

        assertThat(result.isTriggered()).isFalse();
    }

    // -----------------------------------------------------------------------
    // FraudRuleEngine – aggregate behaviour
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Engine: multiple triggered rules → scores accumulate correctly")
    void testRunAnalysis_multipleRules_scoresAccumulate() {
        // HIGH_AMOUNT(30) + WEEKEND(10) + ROUND_AMOUNT(5) = 45  → MEDIUM
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("60000"))  // > 50k AND round multiple of 1000
                .expenseDate(saturday)
                .build();

        FraudAnalysisResult result = engine.runAnalysis(ctx);

        assertThat(result.getTotalRiskScore()).isEqualTo(45);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.getTriggeredRules()).hasSize(3);
    }

    @Test
    @DisplayName("Engine: total score capped at 100 even when rules exceed it")
    void testRunAnalysis_scoreCapAt100() {
        // HIGH_AMOUNT(30) + DUPLICATE(40) + FREQUENT(20) + WEEKEND(10) = 100
        // Adding ROUND(5) would exceed, but cap applies
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("60000"))
                .expenseDate(saturday)
                .isDuplicate(true)
                .monthlyClaimCount(8)
                .build();

        FraudAnalysisResult result = engine.runAnalysis(ctx);

        assertThat(result.getTotalRiskScore()).isLessThanOrEqualTo(100);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("Engine: score <= 30 → LOW risk level")
    void testRunAnalysis_lowRisk_correctLevel() {
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("500"))   // no rules triggered
                .build();

        FraudAnalysisResult result = engine.runAnalysis(ctx);

        assertThat(result.getTotalRiskScore()).isEqualTo(0);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    @DisplayName("Engine: score > 70 → HIGH risk level")
    void testRunAnalysis_highRisk_correctLevel() {
        // DUPLICATE(40) + HIGH_AMOUNT(30) + WEEKEND(10) = 80 → HIGH
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        FraudCheckContext ctx = buildBaseContext()
                .amount(new BigDecimal("75000"))
                .expenseDate(saturday)
                .isDuplicate(true)
                .build();

        FraudAnalysisResult result = engine.runAnalysis(ctx);

        assertThat(result.getTotalRiskScore()).isGreaterThan(70);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.isDuplicate()).isTrue();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private FraudCheckContext.FraudCheckContextBuilder buildBaseContext() {
        return FraudCheckContext.builder()
                .expenseId(1001L)
                .employeeId(42L)
                .amount(new BigDecimal("1500"))
                .merchantName("Test Merchant")
                .expenseDate(LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)))
                .categoryCode("TRAVEL")
                .categoryId(5L)
                .monthlyClaimCount(2)
                .isDuplicate(false);
    }
}
