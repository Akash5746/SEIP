package com.seip.analytics.service;

import com.seip.analytics.dto.*;
import com.seip.analytics.exception.AnalyticsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalyticsService analyticsService;

    // -----------------------------------------------------------------------
    // getOrgSummary tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getOrgSummary returns correct OrgSummaryDto")
    void getOrgSummary_returnsCorrectDto() {
        // Arrange
        OrgSummaryDto expected = new OrgSummaryDto(
                150L,
                new BigDecimal("750000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("600000.00"),
                new BigDecimal("50000.00"),
                35.5,
                8L
        );
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenReturn(expected);

        // Act
        OrgSummaryDto result = analyticsService.getOrgSummary();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.totalExpenses()).isEqualTo(150L);
        assertThat(result.totalAmount()).isEqualByComparingTo("750000.00");
        assertThat(result.approvedAmount()).isEqualByComparingTo("600000.00");
        assertThat(result.rejectedAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.avgRiskScore()).isEqualTo(35.5);
        assertThat(result.highRiskCount()).isEqualTo(8L);

        verify(jdbcTemplate, times(1)).queryForObject(anyString(), any(RowMapper.class));
    }

    @Test
    @DisplayName("getOrgSummary throws AnalyticsException on DB failure")
    void getOrgSummary_throwsAnalyticsException_onDbError() {
        // Arrange
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getOrgSummary())
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("organisation summary");
    }

    // -----------------------------------------------------------------------
    // getMonthlyTrends tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getMonthlyTrends returns list of monthly data for given year")
    @SuppressWarnings("unchecked")
    void getMonthlyTrends_returnsList_forYear() {
        // Arrange
        List<MonthlySpendDto> mockData = List.of(
                new MonthlySpendDto(1, 2024, new BigDecimal("50000"), 12L),
                new MonthlySpendDto(2, 2024, new BigDecimal("75000"), 18L),
                new MonthlySpendDto(3, 2024, new BigDecimal("62000"), 15L)
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2024))).thenReturn(mockData);

        // Act
        List<MonthlySpendDto> result = analyticsService.getMonthlyTrends(2024);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).month()).isEqualTo(1);
        assertThat(result.get(1).totalAmount()).isEqualByComparingTo("75000");
        assertThat(result.get(2).expenseCount()).isEqualTo(15L);

        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(2024));
    }

    @Test
    @DisplayName("getMonthlyTrends throws AnalyticsException on failure")
    @SuppressWarnings("unchecked")
    void getMonthlyTrends_throwsException_onError() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2024)))
                .thenThrow(new RuntimeException("Query timeout"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getMonthlyTrends(2024))
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("2024");
    }

    // -----------------------------------------------------------------------
    // getCategoryBreakdown tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getCategoryBreakdown returns empty list when grand total is zero")
    void getCategoryBreakdown_returnsEmpty_whenNoData() {
        // Arrange
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2024, 12, 31);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(from), eq(to)))
                .thenReturn(BigDecimal.ZERO);

        // Act
        List<CategorySpendDto> result = analyticsService.getCategoryBreakdown(from, to);

        // Assert
        assertThat(result).isEmpty();
        verify(jdbcTemplate, times(1)).queryForObject(anyString(), eq(BigDecimal.class), eq(from), eq(to));
        // No second query should run
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), eq(from), eq(to));
    }

    @Test
    @DisplayName("getCategoryBreakdown throws AnalyticsException on DB failure")
    void getCategoryBreakdown_throwsException_onError() {
        // Arrange
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to   = LocalDate.of(2024, 12, 31);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(from), eq(to)))
                .thenThrow(new RuntimeException("Schema not found"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getCategoryBreakdown(from, to))
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("category breakdown");
    }

    // -----------------------------------------------------------------------
    // getEmployeeReport tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getEmployeeReport returns zero EmployeeReportDto when no data found")
    @SuppressWarnings("unchecked")
    void getEmployeeReport_returnsZeroDto_whenNoDataFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L))).thenReturn(List.of());

        // Act
        EmployeeReportDto result = analyticsService.getEmployeeReport(99L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.employeeId()).isEqualTo(99L);
        assertThat(result.totalClaims()).isZero();
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getEmployeeReport returns correct DTO when data exists")
    @SuppressWarnings("unchecked")
    void getEmployeeReport_returnsCorrectDto_whenDataExists() {
        // Arrange
        EmployeeReportDto expected = new EmployeeReportDto(
                5L,
                new BigDecimal("25000.00"),
                7L,
                new BigDecimal("20000.00"),
                new BigDecimal("3000.00"),
                new BigDecimal("2000.00")
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(5L))).thenReturn(List.of(expected));

        // Act
        EmployeeReportDto result = analyticsService.getEmployeeReport(5L);

        // Assert
        assertThat(result.employeeId()).isEqualTo(5L);
        assertThat(result.totalAmount()).isEqualByComparingTo("25000.00");
        assertThat(result.totalClaims()).isEqualTo(7L);
        assertThat(result.approvedAmount()).isEqualByComparingTo("20000.00");
    }

    // -----------------------------------------------------------------------
    // getFraudTrends tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getFraudTrends returns correct list for given year")
    @SuppressWarnings("unchecked")
    void getFraudTrends_returnsList_forYear() {
        // Arrange
        List<FraudTrendDto> expected = List.of(
                new FraudTrendDto(1, 2024, 50L, 5L, 15L, 30L),
                new FraudTrendDto(2, 2024, 60L, 8L, 12L, 40L)
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2024))).thenReturn(expected);

        // Act
        List<FraudTrendDto> result = analyticsService.getFraudTrends(2024);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).highRisk()).isEqualTo(5L);
        assertThat(result.get(1).totalAnalyzed()).isEqualTo(60L);
        assertThat(result.get(0).lowRisk()).isEqualTo(30L);
    }

    @Test
    @DisplayName("getFraudTrends throws AnalyticsException on DB error")
    @SuppressWarnings("unchecked")
    void getFraudTrends_throwsException_onError() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2024)))
                .thenThrow(new RuntimeException("fraud_schema not found"));

        // Act & Assert
        assertThatThrownBy(() -> analyticsService.getFraudTrends(2024))
                .isInstanceOf(AnalyticsException.class)
                .hasMessageContaining("2024");
    }

    // -----------------------------------------------------------------------
    // getTopSpenders tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getTopSpenders returns correct list with valid limit")
    @SuppressWarnings("unchecked")
    void getTopSpenders_returnsCorrectList() {
        // Arrange
        List<TopSpenderDto> expected = List.of(
                new TopSpenderDto(1L, new BigDecimal("100000"), 25L),
                new TopSpenderDto(2L, new BigDecimal("85000"),  20L),
                new TopSpenderDto(3L, new BigDecimal("70000"),  15L)
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3))).thenReturn(expected);

        // Act
        List<TopSpenderDto> result = analyticsService.getTopSpenders(3);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).employeeId()).isEqualTo(1L);
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo("100000");
        assertThat(result.get(1).claimCount()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getTopSpenders clamps invalid limit to 10")
    @SuppressWarnings("unchecked")
    void getTopSpenders_clampsInvalidLimit_toTen() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10))).thenReturn(List.of());

        // Act — pass invalid limit of 0
        analyticsService.getTopSpenders(0);

        // Assert — should call with clamped limit of 10
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(10));
    }

    @Test
    @DisplayName("getTopSpenders clamps limit > 100 to 10")
    @SuppressWarnings("unchecked")
    void getTopSpenders_clampsOverLimit_toTen() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10))).thenReturn(List.of());

        // Act — pass limit of 999
        analyticsService.getTopSpenders(999);

        // Assert — should call with clamped limit of 10
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(10));
    }
}
