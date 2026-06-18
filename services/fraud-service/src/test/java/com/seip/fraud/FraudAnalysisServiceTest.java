package com.seip.fraud;

import com.seip.fraud.client.ExpenseServiceClient;
import com.seip.fraud.client.MlServiceClient;
import com.seip.fraud.dto.FraudCheckRequestEvent;
import com.seip.fraud.entity.FlagType;
import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.entity.FraudFlag;
import com.seip.fraud.entity.RiskLevel;
import com.seip.fraud.repository.FraudAnalysisRepository;
import com.seip.fraud.rules.*;
import com.seip.fraud.service.FraudAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudAnalysisService – unit tests")
class FraudAnalysisServiceTest {

    @Mock private FraudRuleEngine          fraudRuleEngine;
    @Mock private FraudAnalysisRepository  fraudAnalysisRepository;
    @Mock private ExpenseServiceClient     expenseServiceClient;
    @Mock private MlServiceClient          mlServiceClient;

    @InjectMocks
    private FraudAnalysisService fraudAnalysisService;

    private FraudCheckRequestEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = FraudCheckRequestEvent.builder()
                .expenseId(2001L)
                .employeeId(99L)
                .amount(new BigDecimal("75000"))
                .merchantName("ACME Corp")
                .expenseDate(LocalDate.of(2024, 6, 15))
                .categoryCode("TRAVEL")
                .categoryId(3L)
                .build();
    }

    // -----------------------------------------------------------------------
    // analyzeExpense — saves analysis and flags
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("analyzeExpense: saves FraudAnalysis with correct flags and returns persisted entity")
    void testAnalyzeExpense_savesAnalysisAndFlags() {
        // Arrange — expense-service responses
        when(expenseServiceClient.checkDuplicate(anyLong(), any(BigDecimal.class), anyString()))
                .thenReturn(Mono.just(false));
        when(expenseServiceClient.getMonthlyClaimCount(anyLong()))
                .thenReturn(Mono.just(3));

        // Arrange — ML service
        when(mlServiceClient.predictFraud(any(BigDecimal.class), anyString(), anyInt()))
                .thenReturn(Mono.just(0.5));   // below 0.7 threshold → no ML flag

        // Arrange — rule engine returns HIGH_AMOUNT flag
        RuleResult highAmountResult = new RuleResult(30, FlagType.HIGH_AMOUNT,
                "Amount ₹75000 exceeds ₹50,000 threshold");
        FraudAnalysisResult engineResult = new FraudAnalysisResult(
                2001L, 30, RiskLevel.LOW, false, List.of(highAmountResult));
        when(fraudRuleEngine.runAnalysis(any(FraudCheckContext.class))).thenReturn(engineResult);

        // Arrange — no existing analysis
        when(fraudAnalysisRepository.existsByExpenseId(2001L)).thenReturn(false);

        // Arrange — repository save returns the argument
        when(fraudAnalysisRepository.save(any(FraudAnalysis.class)))
                .thenAnswer(invocation -> {
                    FraudAnalysis saved = invocation.getArgument(0);
                    // Simulate DB-assigned ID
                    return FraudAnalysis.builder()
                            .id(1L)
                            .expenseId(saved.getExpenseId())
                            .employeeId(saved.getEmployeeId())
                            .riskScore(saved.getRiskScore())
                            .riskLevel(saved.getRiskLevel())
                            .isDuplicate(saved.isDuplicate())
                            .mlFraudProbability(saved.getMlFraudProbability())
                            .analystNotes(saved.getAnalystNotes())
                            .flags(saved.getFlags())
                            .build();
                });

        // Act
        FraudAnalysis result = fraudAnalysisService.analyzeExpense(sampleEvent);

        // Assert — entity fields
        assertThat(result).isNotNull();
        assertThat(result.getExpenseId()).isEqualTo(2001L);
        assertThat(result.getEmployeeId()).isEqualTo(99L);
        assertThat(result.getRiskScore()).isEqualTo(30);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.isDuplicate()).isFalse();

        // Assert — flags were attached
        assertThat(result.getFlags()).hasSize(1);
        FraudFlag flag = result.getFlags().get(0);
        assertThat(flag.getFlagType()).isEqualTo(FlagType.HIGH_AMOUNT);
        assertThat(flag.getRiskContribution()).isEqualTo(30);
        assertThat(flag.getFlagDescription()).contains("₹50,000");

        // Assert — interactions
        verify(fraudAnalysisRepository).existsByExpenseId(2001L);
        verify(expenseServiceClient).checkDuplicate(99L, new BigDecimal("75000"), "ACME Corp");
        verify(expenseServiceClient).getMonthlyClaimCount(99L);
        verify(mlServiceClient).predictFraud(new BigDecimal("75000"), "TRAVEL", 3);
        verify(fraudRuleEngine).runAnalysis(any(FraudCheckContext.class));

        // Assert — saved with correct context passed to rule engine
        ArgumentCaptor<FraudCheckContext> contextCaptor = ArgumentCaptor.forClass(FraudCheckContext.class);
        verify(fraudRuleEngine).runAnalysis(contextCaptor.capture());
        FraudCheckContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getExpenseId()).isEqualTo(2001L);
        assertThat(capturedContext.getMonthlyClaimCount()).isEqualTo(3);
        assertThat(capturedContext.isDuplicate()).isFalse();
    }

    // -----------------------------------------------------------------------
    // analyzeExpense — ML flag added when probability > 0.7
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("analyzeExpense: ML probability > 0.7 → ML_DETECTED flag added and score adjusted")
    void testAnalyzeExpense_mlHighProbability_mlFlagAdded() {
        when(expenseServiceClient.checkDuplicate(anyLong(), any(), anyString()))
                .thenReturn(Mono.just(false));
        when(expenseServiceClient.getMonthlyClaimCount(anyLong()))
                .thenReturn(Mono.just(2));
        when(mlServiceClient.predictFraud(any(), anyString(), anyInt()))
                .thenReturn(Mono.just(0.85));  // above threshold → ML_DETECTED flag

        // Rule engine returns score 10 (only WEEKEND rule fired)
        RuleResult weekendResult = new RuleResult(10, FlagType.WEEKEND_SPENDING,
                "Expense on SATURDAY");
        FraudAnalysisResult engineResult = new FraudAnalysisResult(
                2001L, 10, RiskLevel.LOW, false, List.of(weekendResult));
        when(fraudRuleEngine.runAnalysis(any())).thenReturn(engineResult);
        when(fraudAnalysisRepository.existsByExpenseId(2001L)).thenReturn(false);
        when(fraudAnalysisRepository.save(any(FraudAnalysis.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FraudAnalysis result = fraudAnalysisService.analyzeExpense(sampleEvent);

        // 10 (WEEKEND) + 25 (ML) = 35 → MEDIUM
        assertThat(result.getRiskScore()).isEqualTo(35);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.getFlags()).hasSize(2);
        boolean hasMLFlag = result.getFlags().stream()
                .anyMatch(f -> f.getFlagType() == FlagType.ML_DETECTED);
        assertThat(hasMLFlag).isTrue();
        assertThat(result.getMlFraudProbability()).isNotNull();
    }

    // -----------------------------------------------------------------------
    // analyzeExpense — already analyzed → returns existing record
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("analyzeExpense: expense already analyzed → returns existing record without re-running rules")
    void testAnalyzeExpense_alreadyExists_returnsExisting() {
        FraudAnalysis existing = FraudAnalysis.builder()
                .id(5L).expenseId(2001L).employeeId(99L)
                .riskScore(20).riskLevel(RiskLevel.LOW).isDuplicate(false)
                .flags(List.of())
                .build();

        when(fraudAnalysisRepository.existsByExpenseId(2001L)).thenReturn(true);
        when(fraudAnalysisRepository.findByExpenseId(2001L)).thenReturn(Optional.of(existing));

        FraudAnalysis result = fraudAnalysisService.analyzeExpense(sampleEvent);

        assertThat(result.getId()).isEqualTo(5L);
        verifyNoInteractions(fraudRuleEngine, expenseServiceClient, mlServiceClient);
        verify(fraudAnalysisRepository, never()).save(any());
    }
}
