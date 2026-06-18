package com.seip.notification.controller;

import com.seip.notification.dto.ApiResponse;
import com.seip.notification.dto.NotificationLogDto;
import com.seip.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification log management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all notification logs (paginated)",
               description = "Returns paginated notification logs sorted by sentAt descending. Admin/Manager only.")
    public ResponseEntity<ApiResponse<Page<NotificationLogDto>>> getAllLogs(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")             @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction")         @RequestParam(defaultValue = "DESC") String direction) {

        log.info("GET /notifications/logs - page={}, size={}", page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), "sentAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationLogDto> result = notificationService.getAllLogs(pageable);

        return ResponseEntity.ok(ApiResponse.success(
                "Notification logs retrieved successfully", result));
    }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get notification log by ID",
               description = "Returns a single notification log entry. Admin/Manager only.")
    public ResponseEntity<ApiResponse<NotificationLogDto>> getLogById(
            @Parameter(description = "Notification log ID") @PathVariable Long id) {

        log.info("GET /notifications/logs/{}", id);
        NotificationLogDto dto = notificationService.getLogById(id);
        return ResponseEntity.ok(ApiResponse.success("Notification log retrieved successfully", dto));
    }
}
