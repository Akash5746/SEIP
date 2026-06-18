package com.seip.audit.dto;

import java.time.LocalDateTime;

public record AuditEventDto(
        String eventId,
        String userId,
        String username,
        String action,
        String resourceType,
        String resourceId,
        String details,
        String ipAddress,
        String serviceName,
        boolean success,
        String errorMessage,
        LocalDateTime timestamp
) {}
