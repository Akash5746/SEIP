package com.seip.expense;

import com.seip.expense.dto.ReceiptDto;
import com.seip.expense.entity.Expense;
import com.seip.expense.entity.ExpenseCategory;
import com.seip.expense.entity.ExpenseStatus;
import com.seip.expense.entity.Receipt;
import com.seip.expense.exception.FileStorageException;
import com.seip.expense.mapper.ExpenseMapper;
import com.seip.expense.repository.ExpenseRepository;
import com.seip.expense.repository.ReceiptRepository;
import com.seip.expense.service.MinioStorageService;
import com.seip.expense.service.ReceiptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Unit Tests")
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ReceiptService receiptService;

    private Expense testExpense;
    private Receipt testReceipt;
    private ReceiptDto testReceiptDto;

    @BeforeEach
    void setUp() {
        // Inject @Value field
        ReflectionTestUtils.setField(receiptService, "bucketName", "expense-receipts");

        ExpenseCategory category = ExpenseCategory.builder()
                .id(1L)
                .name("Travel")
                .code("TRAVEL")
                .isActive(true)
                .build();

        testExpense = Expense.builder()
                .id(1L)
                .expenseNumber("EXP-2026-000001")
                .employeeId(100L)
                .category(category)
                .title("Business Travel")
                .amount(new BigDecimal("5000.00"))
                .expenseDate(LocalDate.now())
                .status(ExpenseStatus.DRAFT)
                .items(new ArrayList<>())
                .receipts(new ArrayList<>())
                .build();

        testReceipt = Receipt.builder()
                .id(1L)
                .expense(testExpense)
                .fileName("receipt.jpg")
                .fileUrl("http://localhost:9000/expense-receipts/expenses/1/receipt.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .uploadTime(LocalDateTime.now())
                .build();

        testReceiptDto = ReceiptDto.builder()
                .id(1L)
                .fileName("receipt.jpg")
                .fileUrl("http://localhost:9000/expense-receipts/expenses/1/receipt.jpg")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // Upload Receipt Tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("testUploadReceipt_callsMinIO")
    void testUploadReceipt_callsMinIO() {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                new byte[1024]
        );

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        when(minioStorageService.uploadFile(
                anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("http://localhost:9000/expense-receipts/expenses/1/uuid_receipt.jpg");
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);
        when(expenseMapper.toReceiptDto(any(Receipt.class))).thenReturn(testReceiptDto);

        // Act
        ReceiptDto result = receiptService.uploadReceipt(1L, 100L, mockFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("receipt.jpg");
        verify(minioStorageService).uploadFile(
                eq("expense-receipts"),
                anyString(),
                any(InputStream.class),
                eq(1024L),
                eq("image/jpeg"));
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    @DisplayName("testUploadReceipt_invalidFileType_throwsException")
    void testUploadReceipt_invalidFileType_throwsException() {
        // Arrange - executable file type (not allowed)
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                new byte[512]
        );

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

        // Act & Assert
        assertThatThrownBy(() -> receiptService.uploadReceipt(1L, 100L, invalidFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file type");

        verify(minioStorageService, never()).uploadFile(any(), any(), any(), anyLong(), any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("testUploadReceipt_validPdf_success")
    void testUploadReceipt_validPdf_success() {
        // Arrange
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                new byte[2048]
        );

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        when(minioStorageService.uploadFile(
                anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("http://localhost:9000/expense-receipts/expenses/1/uuid_invoice.pdf");
        when(receiptRepository.save(any(Receipt.class))).thenReturn(testReceipt);
        when(expenseMapper.toReceiptDto(any())).thenReturn(testReceiptDto);

        // Act
        ReceiptDto result = receiptService.uploadReceipt(1L, 100L, pdfFile);

        // Assert
        assertThat(result).isNotNull();
        verify(minioStorageService).uploadFile(anyString(), anyString(), any(), anyLong(), eq("application/pdf"));
    }

    @Test
    @DisplayName("testUploadReceipt_exceedsSizeLimit_throwsException")
    void testUploadReceipt_exceedsSizeLimit_throwsException() {
        // Arrange - file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

        // Act & Assert
        assertThatThrownBy(() -> receiptService.uploadReceipt(1L, 100L, largeFile))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("10MB");

        verify(minioStorageService, never()).uploadFile(any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("testUploadReceipt_wrongOwner_throwsAccessDenied")
    void testUploadReceipt_wrongOwner_throwsAccessDenied() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.jpg", "image/jpeg", new byte[512]);

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

        // employeeId 999 is NOT the owner (owner is 100)
        assertThatThrownBy(() -> receiptService.uploadReceipt(1L, 999L, file))
                .isInstanceOf(com.seip.expense.exception.AccessDeniedException.class);

        verify(minioStorageService, never()).uploadFile(any(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("testUploadReceipt_minioFailure_throwsFileStorageException")
    void testUploadReceipt_minioFailure_throwsFileStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", new byte[512]);

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        when(minioStorageService.uploadFile(any(), any(), any(), anyLong(), any()))
                .thenThrow(new FileStorageException("MinIO connection refused"));

        assertThatThrownBy(() -> receiptService.uploadReceipt(1L, 100L, file))
                .isInstanceOf(FileStorageException.class);

        verify(receiptRepository, never()).save(any());
    }
}
