package com.seip.expense.dto;

public record ReceiptContentDto(
        String fileName,
        String contentType,
        byte[] content
) {
}
