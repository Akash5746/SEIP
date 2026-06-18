package com.seip.expense;

import com.seip.expense.dto.CreateExpenseRequest;
import com.seip.expense.dto.ExpenseDto;
import com.seip.expense.dto.SubmitExpenseResponse;
import com.seip.expense.dto.UpdateExpenseRequest;
import com.seip.expense.entity.*;
import com.seip.expense.exception.InvalidExpenseStateException;
import com.seip.expense.exception.ResourceNotFoundException;
import com.seip.expense.kafka.ExpenseEventPublisher;
import com.seip.expense.mapper.ExpenseMapper;
import com.seip.expense.repository.ExpenseRepository;
import com.seip.expense.service.ExpenseCategoryService;
import com.seip.expense.service.ExpenseService;
import com.seip.expense.util.ExpenseNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseCategoryService categoryService;

    @Mock
    private ExpenseNumberGenerator expenseNumberGenerator;

    @Mock
    private ExpenseEventPublisher eventPublisher;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseCategory testCategory;
    private Expense draftExpense;
    private Expense submittedExpense;
    private ExpenseDto expenseDto;

    @BeforeEach
    void setUp() {
        testCategory = ExpenseCategory.builder()
                .id(1L)
                .name("Travel")
                .code("TRAVEL")
                .description("Business travel expenses")
                .isActive(true)
                .build();

        draftExpense = Expense.builder()
                .id(1L)
                .expenseNumber("EXP-2026-000001")
                .employeeId(100L)
                .category(testCategory)
                .title("Business Travel to Mumbai")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .expenseDate(LocalDate.now())
                .status(ExpenseStatus.DRAFT)
                .riskScore(0)
                .riskLevel("LOW")
                .items(new ArrayList<>())
                .receipts(new ArrayList<>())
                .build();

        submittedExpense = Expense.builder()
                .id(2L)
                .expenseNumber("EXP-2026-000002")
                .employeeId(100L)
                .category(testCategory)
                .title("Hotel Stay")
                .amount(new BigDecimal("3000.00"))
                .currency("INR")
                .expenseDate(LocalDate.now())
                .status(ExpenseStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .riskScore(0)
                .riskLevel("LOW")
                .items(new ArrayList<>())
                .receipts(new ArrayList<>())
                .build();

        expenseDto = ExpenseDto.builder()
                .id(1L)
                .expenseNumber("EXP-2026-000001")
                .employeeId(100L)
                .title("Business Travel to Mumbai")
                .amount(new BigDecimal("5000.00"))
                .status(ExpenseStatus.DRAFT)
                .build();
    }

    // -----------------------------------------------------------------------
    // Create Expense Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testCreateExpense_generatesUniqueExpenseNumber")
    void testCreateExpense_generatesUniqueExpenseNumber() {
        // Arrange
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Business Travel to Mumbai")
                .amount(new BigDecimal("5000.00"))
                .expenseDate(LocalDate.now())
                .categoryId(1L)
                .currency("INR")
                .build();

        when(categoryService.findEntityById(1L)).thenReturn(testCategory);
        when(expenseNumberGenerator.generateExpenseNumber()).thenReturn("EXP-2026-000001");
        when(expenseRepository.save(any(Expense.class))).thenReturn(draftExpense);
        when(expenseMapper.toDto(any(Expense.class))).thenReturn(expenseDto);

        // Act
        ExpenseDto result = expenseService.createExpense(100L, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExpenseNumber()).isEqualTo("EXP-2026-000001");
        verify(expenseNumberGenerator, times(1)).generateExpenseNumber();
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("testCreateExpense_setsStatusToDraft")
    void testCreateExpense_setsStatusToDraft() {
        // Arrange
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Test Expense")
                .amount(new BigDecimal("100.00"))
                .expenseDate(LocalDate.now())
                .categoryId(1L)
                .build();

        when(categoryService.findEntityById(1L)).thenReturn(testCategory);
        when(expenseNumberGenerator.generateExpenseNumber()).thenReturn("EXP-2026-000001");
        when(expenseRepository.save(argThat(expense ->
                expense.getStatus() == ExpenseStatus.DRAFT))).thenReturn(draftExpense);
        when(expenseMapper.toDto(any())).thenReturn(expenseDto);

        // Act
        expenseService.createExpense(100L, request);

        // Assert - verify save was called with DRAFT status
        verify(expenseRepository).save(argThat(expense ->
                expense.getStatus() == ExpenseStatus.DRAFT));
    }

    @Test
    @DisplayName("testCreateExpense_categoryNotFound_throwsException")
    void testCreateExpense_categoryNotFound_throwsException() {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .title("Test")
                .amount(new BigDecimal("100.00"))
                .expenseDate(LocalDate.now())
                .categoryId(999L)
                .build();

        when(categoryService.findEntityById(999L))
                .thenThrow(new ResourceNotFoundException("ExpenseCategory", 999L));

        assertThatThrownBy(() -> expenseService.createExpense(100L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -----------------------------------------------------------------------
    // Submit Expense Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testSubmitExpense_changeStatusToSubmitted")
    void testSubmitExpense_changeStatusToSubmitted() {
        // Arrange
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(draftExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventPublisher).publishExpenseSubmitted(any());
        doNothing().when(eventPublisher).publishFraudCheckRequest(any());

        // Act
        SubmitExpenseResponse response = expenseService.submitExpense(1L, 100L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ExpenseStatus.SUBMITTED);
        assertThat(response.getExpenseNumber()).isEqualTo("EXP-2026-000001");
        verify(expenseRepository).save(argThat(e -> e.getStatus() == ExpenseStatus.SUBMITTED));
        verify(eventPublisher).publishExpenseSubmitted(any());
        verify(eventPublisher).publishFraudCheckRequest(any());
    }

    @Test
    @DisplayName("testSubmitExpense_alreadySubmitted_throwsException")
    void testSubmitExpense_alreadySubmitted_throwsException() {
        // Arrange
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(submittedExpense));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.submitExpense(2L, 100L))
                .isInstanceOf(InvalidExpenseStateException.class)
                .hasMessageContaining("SUBMITTED");

        verify(expenseRepository, never()).save(any());
        verify(eventPublisher, never()).publishExpenseSubmitted(any());
    }

    // -----------------------------------------------------------------------
    // Update Expense Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testUpdateExpense_draftStatus_success")
    void testUpdateExpense_draftStatus_success() {
        // Arrange
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("Updated Travel Expense")
                .amount(new BigDecimal("6000.00"))
                .build();

        ExpenseDto updatedDto = ExpenseDto.builder()
                .id(1L)
                .title("Updated Travel Expense")
                .amount(new BigDecimal("6000.00"))
                .status(ExpenseStatus.DRAFT)
                .build();

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(draftExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(draftExpense);
        when(expenseMapper.toDto(any())).thenReturn(updatedDto);

        // Act
        ExpenseDto result = expenseService.updateExpense(1L, 100L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("testUpdateExpense_submittedStatus_throwsException")
    void testUpdateExpense_submittedStatus_throwsException() {
        // Arrange
        UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                .title("Should fail")
                .build();

        when(expenseRepository.findById(2L)).thenReturn(Optional.of(submittedExpense));

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(2L, 100L, request))
                .isInstanceOf(InvalidExpenseStateException.class)
                .hasMessageContaining("SUBMITTED");

        verify(expenseRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Approve / Reject Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testApproveExpense_success")
    void testApproveExpense_success() {
        // Arrange
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(submittedExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventPublisher).publishExpenseApproved(any(), anyLong(), anyString());

        ExpenseDto approvedDto = ExpenseDto.builder()
                .id(2L)
                .status(ExpenseStatus.APPROVED)
                .build();
        when(expenseMapper.toDto(any())).thenReturn(approvedDto);

        // Act
        ExpenseDto result = expenseService.approveExpense(2L, 200L, "Approved by manager");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        verify(expenseRepository).save(argThat(e -> e.getStatus() == ExpenseStatus.APPROVED
                && e.getReviewerId().equals(200L)));
        verify(eventPublisher).publishExpenseApproved(any(), eq(200L), eq("Approved by manager"));
    }

    @Test
    @DisplayName("testRejectExpense_withNotes")
    void testRejectExpense_withNotes() {
        // Arrange
        String rejectionNotes = "Receipt is missing. Please resubmit with valid receipts.";
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(submittedExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(eventPublisher).publishExpenseRejected(any(), anyLong(), anyString());

        ExpenseDto rejectedDto = ExpenseDto.builder()
                .id(2L)
                .status(ExpenseStatus.REJECTED)
                .reviewNotes(rejectionNotes)
                .build();
        when(expenseMapper.toDto(any())).thenReturn(rejectedDto);

        // Act
        ExpenseDto result = expenseService.rejectExpense(2L, 200L, rejectionNotes);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ExpenseStatus.REJECTED);
        assertThat(result.getReviewNotes()).isEqualTo(rejectionNotes);
        verify(expenseRepository).save(argThat(e ->
                e.getStatus() == ExpenseStatus.REJECTED
                && rejectionNotes.equals(e.getReviewNotes())));
        verify(eventPublisher).publishExpenseRejected(any(), eq(200L), eq(rejectionNotes));
    }

    @Test
    @DisplayName("testApproveExpense_draftStatus_throwsException")
    void testApproveExpense_draftStatus_throwsException() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(draftExpense));

        assertThatThrownBy(() -> expenseService.approveExpense(1L, 200L, "notes"))
                .isInstanceOf(InvalidExpenseStateException.class)
                .hasMessageContaining("DRAFT");
    }

    // -----------------------------------------------------------------------
    // Delete Expense Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testDeleteExpense_draftStatus_success")
    void testDeleteExpense_draftStatus_success() {
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(draftExpense));

        assertThatCode(() -> expenseService.deleteExpense(1L, 100L))
                .doesNotThrowAnyException();

        verify(expenseRepository).delete(draftExpense);
    }

    @Test
    @DisplayName("testDeleteExpense_submittedStatus_throwsException")
    void testDeleteExpense_submittedStatus_throwsException() {
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(submittedExpense));

        assertThatThrownBy(() -> expenseService.deleteExpense(2L, 100L))
                .isInstanceOf(InvalidExpenseStateException.class);

        verify(expenseRepository, never()).delete(any());
    }
}
