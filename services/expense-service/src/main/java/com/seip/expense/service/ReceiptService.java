package com.seip.expense.service;

import com.seip.expense.dto.ReceiptDto;
import com.seip.expense.entity.Expense;
import com.seip.expense.entity.Receipt;
import com.seip.expense.exception.AccessDeniedException;
import com.seip.expense.exception.FileStorageException;
import com.seip.expense.exception.ResourceNotFoundException;
import com.seip.expense.mapper.ExpenseMapper;
import com.seip.expense.repository.ExpenseRepository;
import com.seip.expense.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private final ReceiptRepository receiptRepository;
    private final ExpenseRepository expenseRepository;
    private final MinioStorageService minioStorageService;
    private final ExpenseMapper expenseMapper;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Transactional
    public ReceiptDto uploadReceipt(Long expenseId, Long employeeId, MultipartFile file) {
        log.info("Uploading receipt for expense {} by employee {}", expenseId, employeeId);

        // Validate expense ownership
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        if (!expense.getEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("You do not have permission to upload receipts for this expense");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 10MB");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "Invalid file type. Allowed types: image/jpeg, image/png, application/pdf");
        }

        // Build unique object name
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "receipt";
        String objectName = String.format("expenses/%d/%s_%s",
                expenseId, UUID.randomUUID(), originalFileName);

        // Upload to MinIO
        String fileUrl;
        try (InputStream inputStream = file.getInputStream()) {
            fileUrl = minioStorageService.uploadFile(
                    bucketName, objectName, inputStream, file.getSize(), contentType);
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload receipt to storage: " + e.getMessage(), e);
        }

        // Save receipt entity
        Receipt receipt = Receipt.builder()
                .expense(expense)
                .fileName(originalFileName)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(contentType)
                .uploadTime(LocalDateTime.now())
                .build();

        Receipt saved = receiptRepository.save(receipt);
        log.info("Receipt {} uploaded for expense {}", saved.getId(), expenseId);
        return expenseMapper.toReceiptDto(saved);
    }

    public List<ReceiptDto> getReceiptsByExpense(Long expenseId) {
        log.debug("Fetching receipts for expense {}", expenseId);
        List<Receipt> receipts = receiptRepository.findByExpenseId(expenseId);
        return expenseMapper.toReceiptDtoList(receipts);
    }

    @Transactional
    public void deleteReceipt(Long receiptId, Long employeeId) {
        log.info("Deleting receipt {} for employee {}", receiptId, employeeId);
        Receipt receipt = receiptRepository.findByIdAndExpenseEmployeeId(receiptId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found or you do not have permission to delete it"));
        receiptRepository.delete(receipt);
        log.info("Deleted receipt {}", receiptId);
    }
}
