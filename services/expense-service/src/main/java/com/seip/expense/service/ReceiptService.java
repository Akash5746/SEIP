package com.seip.expense.service;

import com.seip.expense.dto.ReceiptDto;
import com.seip.expense.dto.ReceiptContentDto;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private final ReceiptRepository receiptRepository;
    private final ExpenseRepository expenseRepository;
    private final MinioStorageService minioStorageService;
    private final ExpenseMapper expenseMapper;
    private final JdbcTemplate jdbcTemplate;

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
                    "Invalid file type. Allowed types: image/jpeg, image/png, image/webp, application/pdf");
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
        return withAccessUrl(expenseMapper.toReceiptDto(saved));
    }

    public List<ReceiptDto> getReceiptsByExpense(Long expenseId) {
        log.debug("Fetching receipts for expense {}", expenseId);
        List<Receipt> receipts = receiptRepository.findByExpenseId(expenseId);
        return withAccessUrls(expenseMapper.toReceiptDtoList(receipts));
    }

    public ReceiptContentDto getReceiptContent(
            Long expenseId,
            Long receiptId,
            Long requesterAuthUserId,
            String requesterRole) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", receiptId));

        if (!receipt.getExpense().getId().equals(expenseId)) {
            throw new ResourceNotFoundException("Receipt", receiptId);
        }

        validateReceiptAccess(receipt.getExpense(), requesterAuthUserId, requesterRole);

        String objectName = extractObjectName(receipt.getFileUrl());
        byte[] content = minioStorageService.getFileBytes(bucketName, objectName);
        return new ReceiptContentDto(receipt.getFileName(), receipt.getContentType(), content);
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

    public ReceiptDto withAccessUrl(ReceiptDto receiptDto) {
        if (receiptDto == null || receiptDto.getFileUrl() == null || receiptDto.getFileUrl().isBlank()) {
            return receiptDto;
        }

        long expenseId = extractExpenseId(receiptDto.getFileUrl());
        receiptDto.setFileUrl(buildReceiptContentPath(expenseId, receiptDto.getId()));
        return receiptDto;
    }

    public List<ReceiptDto> withAccessUrls(List<ReceiptDto> receipts) {
        return receipts.stream()
                .map(this::withAccessUrl)
                .toList();
    }

    private String extractObjectName(String fileUrl) {
        String normalizedUrl = fileUrl.trim();
        int queryIndex = normalizedUrl.indexOf('?');
        if (queryIndex >= 0) {
            normalizedUrl = normalizedUrl.substring(0, queryIndex);
        }

        String bucketPath = "/" + bucketName + "/";
        int bucketIndex = normalizedUrl.toLowerCase(Locale.ROOT).indexOf(bucketPath.toLowerCase(Locale.ROOT));
        if (bucketIndex < 0) {
            throw new FileStorageException("Stored file URL is missing the expected bucket path");
        }

        return normalizedUrl.substring(bucketIndex + bucketPath.length());
    }

    private long extractExpenseId(String fileUrl) {
        String objectName = extractObjectName(fileUrl);
        String[] parts = objectName.split("/", 3);
        if (parts.length < 3 || !"expenses".equalsIgnoreCase(parts[0])) {
            throw new FileStorageException("Stored file URL is missing the expected expense path");
        }

        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            throw new FileStorageException("Stored file URL contains an invalid expense path", ex);
        }
    }

    private String buildReceiptContentPath(Long expenseId, Long receiptId) {
        return "/expenses/" + expenseId + "/receipts/" + receiptId + "/content";
    }

    private void validateReceiptAccess(Expense expense, Long requesterAuthUserId, String requesterRole) {
        if (expense.getEmployeeId().equals(requesterAuthUserId)) {
            return;
        }

        if (isAdmin(requesterRole)) {
            return;
        }

        if (!isManager(requesterRole)) {
            throw new AccessDeniedException("You do not have permission to access this receipt");
        }

        if (!isEmployeeAuthUser(expense.getEmployeeId())) {
            throw new AccessDeniedException("Managers can only access employee receipts");
        }

        Long requesterDepartmentId = getDepartmentIdForAuthUser(requesterAuthUserId);
        Long employeeDepartmentId = getDepartmentIdForAuthUser(expense.getEmployeeId());

        if (requesterDepartmentId == null || employeeDepartmentId == null
                || !requesterDepartmentId.equals(employeeDepartmentId)) {
            throw new AccessDeniedException("You can only access receipts for employees in your department");
        }
    }

    private boolean isEmployeeAuthUser(Long authUserId) {
        List<Boolean> results = jdbcTemplate.query("""
                SELECT EXISTS (
                    SELECT 1
                    FROM auth.users u
                    JOIN auth.user_roles ur ON ur.user_id = u.id
                    JOIN auth.roles r ON r.id = ur.role_id
                    WHERE u.id = ?
                      AND u.enabled = true
                      AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                ) AS is_employee
                """, (rs, rowNum) -> rs.getBoolean("is_employee"), authUserId);

        return !results.isEmpty() && Boolean.TRUE.equals(results.get(0));
    }

    private Long getDepartmentIdForAuthUser(Long authUserId) {
        List<Long> results = jdbcTemplate.query("""
                SELECT department_id
                FROM users.employees
                WHERE auth_user_id = ?
                LIMIT 1
                """, (rs, rowNum) -> {
            long value = rs.getLong("department_id");
            return rs.wasNull() ? null : value;
        }, authUserId);

        return results.isEmpty() ? null : results.get(0);
    }

    private boolean isManager(String role) {
        return "ROLE_MANAGER".equals(normalizeRole(role));
    }

    private boolean isAdmin(String role) {
        return "ROLE_ADMIN".equals(normalizeRole(role));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
