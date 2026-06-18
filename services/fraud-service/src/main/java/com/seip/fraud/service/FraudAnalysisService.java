package com.seip.fraud.service;

import com.seip.fraud.client.ExpenseServiceClient;
import com.seip.fraud.client.MlServiceClient;
import com.seip.fraud.dto.FraudAnalysisDto;
import com.seip.fraud.dto.FraudCheckRequestEvent;
import com.seip.fraud.dto.FraudDashboardDto;
import com.seip.fraud.dto.FraudFlagDto;
import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.entity.FraudFlag;
import com.seip.fraud.entity.RiskLevel;
import com.seip.fraud.exception.ResourceNotFoundException;
import com.seip.fraud.repository.FraudAnalysisRepository;
import com.seip.fraud.rules.FraudAnalysisResult;
import com.seip.fraud.rules.FraudCheckContext;
import com.seip.fraud.rules.FraudRuleEngine;
import com.seip.fraud.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private static final double ML_FRAUD_THRESHOLD = 0.7;

    private final FraudRuleEngine       fraudRuleEngine;
    private final FraudAnalysisRepository fraudAnalysisRepository;
    private final ExpenseServiceClient  expenseServiceClient;
    private final MlServiceClient       mlServiceClient;

    // -----------------------------------------------------------------------
    // Analyze
    // -----------------------------------------------------------------------

    /**
     * Full fraud analysis pipeline:
     * 1. Fetch enrichment data (duplicate flag, monthly count) from expense-service
     * 2. Run rule engine
     * 3. Optionally call ML service and add ML_DETECTED flag
     * 4. Persist FraudAnalysis + FraudFlags
     */
    @Transactional
    public FraudAnalysis analyzeExpense(FraudCheckRequestEvent event) {
        log.info("Starting fraud analysis for expenseId={}, employeeId={}",
                event.getExpenseId(), event.getEmployeeId());

        // Skip re-analysis if already processed
        if (fraudAnalysisRepository.existsByExpenseId(event.getExpenseId())) {
            log.warn("Fraud analysis already exists for expenseId={}; returning existing record.",
                    event.getExpenseId());
            return fraudAnalysisRepository.findByExpenseId(event.getExpenseId()).orElseThrow();
        }

        // 1. Fetch enrichment data reactively (block is acceptable in a Kafka consumer thread)
        Boolean isDuplicate = expenseServiceClient
                .checkDuplicate(event.getEmployeeId(), event.getAmount(), event.getMerchantName())
                .block();

        Integer monthlyClaimCount = expenseServiceClient
                .getMonthlyClaimCount(event.getEmployeeId())
                .block();

        // 2. Build context
        FraudCheckContext context = FraudCheckContext.builder()
                .expenseId(event.getExpenseId())
                .employeeId(event.getEmployeeId())
                .amount(event.getAmount())
                .merchantName(event.getMerchantName())
                .expenseDate(event.getExpenseDate())
                .categoryCode(event.getCategoryCode())
                .categoryId(event.getCategoryId())
                .isDuplicate(Boolean.TRUE.equals(isDuplicate))
                .monthlyClaimCount(monthlyClaimCount != null ? monthlyClaimCount : 0)
                .build();

        // 3. Run rule engine
        FraudAnalysisResult engineResult = fraudRuleEngine.runAnalysis(context);

        // 4. Call ML service
        Double mlProbability = mlServiceClient
                .predictFraud(event.getAmount(), event.getCategoryCode(), context.getMonthlyClaimCount())
                .block();

        // Consolidate rule results
        List<RuleResult> allTriggeredRules = new ArrayList<>(engineResult.getTriggeredRules());
        int totalScore = engineResult.getTotalRiskScore();

        if (mlProbability != null && mlProbability > ML_FRAUD_THRESHOLD) {
            log.info("ML model flagged expenseId={} with probability={}", event.getExpenseId(), mlProbability);
            RuleResult mlFlag = new RuleResult(
                    25,
                    FlagType.ML_DETECTED,
                    "ML model predicted fraud probability of " + String.format("%.2f%%", mlProbability * 100)
            );
            allTriggeredRules.add(mlFlag);
            totalScore = Math.min(totalScore + 25, 100);
        }

        // Re-compute risk level after potential ML score addition
        RiskLevel finalLevel = computeRiskLevel(totalScore);

        // 5. Build and persist entity
        FraudAnalysis analysis = FraudAnalysis.builder()
                .expenseId(event.getExpenseId())
                .employeeId(event.getEmployeeId())
                .riskScore(totalScore)
                .riskLevel(finalLevel)
                .isDuplicate(engineResult.isDuplicate())
                .mlFraudProbability(mlProbability != null
                        ? BigDecimal.valueOf(mlProbability).setScale(4, RoundingMode.HALF_UP) : null)
                .analystNotes(buildAnalystNotes(allTriggeredRules, finalLevel))
                .flags(new ArrayList<>())
                .build();

        // Build flag entities
        List<FraudFlag> fraudFlags = allTriggeredRules.stream()
                .map(ruleResult -> FraudFlag.builder()
                        .analysis(analysis)
                        .flagType(ruleResult.getFlagType())
                        .flagDescription(ruleResult.getDescription())
                        .riskContribution(ruleResult.getRiskScore())
                        .build())
                .collect(Collectors.toList());

        analysis.getFlags().addAll(fraudFlags);

        FraudAnalysis saved = fraudAnalysisRepository.save(analysis);
        log.info("Fraud analysis saved: id={}, expenseId={}, riskLevel={}, score={}",
                saved.getId(), saved.getExpenseId(), saved.getRiskLevel(), saved.getRiskScore());
        return saved;
    }

    // -----------------------------------------------------------------------
    // Query methods
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public FraudAnalysisDto getAnalysisByExpenseId(Long expenseId) {
        FraudAnalysis analysis = fraudAnalysisRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FraudAnalysis", "expenseId", expenseId));
        return toDto(analysis);
    }

    @Transactional(readOnly = true)
    public FraudDashboardDto getFraudDashboard() {
        long total       = fraudAnalysisRepository.count();
        long highRisk    = fraudAnalysisRepository.countByRiskLevel(RiskLevel.HIGH);
        long mediumRisk  = fraudAnalysisRepository.countByRiskLevel(RiskLevel.MEDIUM);
        long lowRisk     = fraudAnalysisRepository.countByRiskLevel(RiskLevel.LOW);
        long duplicates  = fraudAnalysisRepository.countByIsDuplicateTrue();

        BigDecimal fraudRate = total > 0
                ? BigDecimal.valueOf((double) (highRisk + mediumRisk) / total * 100)
                      .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return FraudDashboardDto.builder()
                .totalAnalyzed(total)
                .highRiskCount(highRisk)
                .mediumRiskCount(mediumRisk)
                .lowRiskCount(lowRisk)
                .duplicatesDetected(duplicates)
                .fraudRatePercent(fraudRate)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<FraudAnalysisDto> getHighRiskExpenses(Pageable pageable) {
        Page<FraudAnalysis> page = fraudAnalysisRepository.findByRiskLevel(RiskLevel.HIGH, pageable);
        List<FraudAnalysisDto> dtos = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private RiskLevel computeRiskLevel(int score) {
        if (score <= 30) return RiskLevel.LOW;
        if (score <= 70) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    private String buildAnalystNotes(List<RuleResult> triggered, RiskLevel level) {
        if (triggered.isEmpty()) {
            return "No fraud indicators detected. Risk level: " + level;
        }
        String flagSummary = triggered.stream()
                .map(r -> r.getFlagType().name())
                .collect(Collectors.joining(", "));
        return "Triggered flags: [" + flagSummary + "]. Risk level: " + level;
    }

    private FraudAnalysisDto toDto(FraudAnalysis a) {
        List<FraudFlagDto> flagDtos = a.getFlags().stream()
                .map(f -> FraudFlagDto.builder()
                        .id(f.getId())
                        .flagType(f.getFlagType().name())
                        .flagDescription(f.getFlagDescription())
                        .riskContribution(f.getRiskContribution())
                        .build())
                .collect(Collectors.toList());

        return FraudAnalysisDto.builder()
                .id(a.getId())
                .expenseId(a.getExpenseId())
                .employeeId(a.getEmployeeId())
                .riskScore(a.getRiskScore())
                .riskLevel(a.getRiskLevel().name())
                .isDuplicate(a.isDuplicate())
                .analysisTime(a.getAnalysisTime())
                .mlFraudProbability(a.getMlFraudProbability())
                .analystNotes(a.getAnalystNotes())
                .flags(flagDtos)
                .build();
    }
}
