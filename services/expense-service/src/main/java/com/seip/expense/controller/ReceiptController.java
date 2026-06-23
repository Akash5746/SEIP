package com.seip.expense.controller;

import com.seip.expense.dto.ApiResponse;
import com.seip.expense.dto.ReceiptContentDto;
import com.seip.expense.dto.ReceiptDto;
import com.seip.expense.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/expenses/{expenseId}/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipt Management", description = "APIs for uploading and managing expense receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a receipt file for an expense")
    public ResponseEntity<ApiResponse<ReceiptDto>> uploadReceipt(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long expenseId,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /expenses/{}/receipts - employeeId={}, fileName={}",
                expenseId, employeeId, file.getOriginalFilename());
        ReceiptDto dto = receiptService.uploadReceipt(expenseId, employeeId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dto, "Receipt uploaded successfully"));
    }

    @GetMapping
    @Operation(summary = "List all receipts for an expense")
    public ResponseEntity<ApiResponse<List<ReceiptDto>>> getReceipts(
            @PathVariable Long expenseId) {
        List<ReceiptDto> receipts = receiptService.getReceiptsByExpense(expenseId);
        return ResponseEntity.ok(ApiResponse.success(receipts));
    }

    @GetMapping("/{receiptId}/content")
    @Operation(summary = "Open a receipt file inline")
    public ResponseEntity<byte[]> getReceiptContent(
            @RequestHeader("X-Auth-User-Id") Long requesterAuthUserId,
            @RequestHeader(value = "X-Auth-User-Role", required = false) String requesterRole,
            @PathVariable Long expenseId,
            @PathVariable Long receiptId) {
        ReceiptContentDto content = receiptService.getReceiptContent(
                expenseId,
                receiptId,
                requesterAuthUserId,
                requesterRole
        );

        MediaType mediaType = content.contentType() != null && !content.contentType().isBlank()
                ? MediaType.parseMediaType(content.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(content.fileName()).build().toString())
                .body(content.content());
    }

    @DeleteMapping("/{receiptId}")
    @Operation(summary = "Delete a receipt")
    public ResponseEntity<ApiResponse<Void>> deleteReceipt(
            @RequestHeader("X-Auth-User-Id") Long employeeId,
            @PathVariable Long expenseId,
            @PathVariable Long receiptId) {
        receiptService.deleteReceipt(receiptId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Receipt deleted successfully"));
    }
}
