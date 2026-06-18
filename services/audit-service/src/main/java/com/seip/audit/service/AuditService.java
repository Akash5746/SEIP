package com.seip.audit.service;

import com.seip.audit.dto.AuditEventDto;
import com.seip.audit.dto.PageResponse;
import com.seip.audit.entity.AuditLog;
import com.seip.audit.exception.AuditLogNotFoundException;
import com.seip.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Returns paginated audit logs with optional filters.
     */
    public PageResponse<AuditEventDto> getLogs(Pageable pageable,
                                               String userId,
                                               String action,
                                               LocalDateTime from,
                                               LocalDateTime to) {
        log.debug("Fetching audit logs - userId={}, action={}, from={}, to={}", userId, action, from, to);

        Page<AuditLog> page = auditLogRepository.findWithFilters(userId, action, from, to, pageable);
        Page<AuditEventDto> dtoPage = page.map(this::toDto);
        return PageResponse.from(dtoPage);
    }

    /**
     * Returns all audit logs for a given expenseId (resourceId).
     */
    public List<AuditEventDto> getLogsByExpenseId(String expenseId) {
        log.debug("Fetching audit logs by expenseId={}", expenseId);
        List<AuditLog> logs = auditLogRepository.findByResourceIdAndResourceType(expenseId, "EXPENSE");
        if (logs.isEmpty()) {
            log.info("No audit logs found for expenseId={}", expenseId);
        }
        return logs.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Returns paginated audit logs for a given userId.
     */
    public PageResponse<AuditEventDto> getLogsByUserId(String userId, Pageable pageable) {
        log.debug("Fetching audit logs by userId={}", userId);
        Page<AuditLog> page = auditLogRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    /**
     * Saves a manually created audit event (e.g. from internal API calls).
     */
    @Transactional
    public AuditLog saveAuditLog(AuditEventDto dto) {
        log.info("Saving manual audit log for action={}, resourceId={}", dto.action(), dto.resourceId());
        AuditLog entity = AuditLog.builder()
                .eventId(dto.eventId() != null ? dto.eventId() : UUID.randomUUID().toString())
                .userId(dto.userId())
                .username(dto.username())
                .action(dto.action())
                .resourceType(dto.resourceType())
                .resourceId(dto.resourceId())
                .details(dto.details())
                .ipAddress(dto.ipAddress())
                .userAgent(null)
                .serviceName(dto.serviceName())
                .success(dto.success())
                .errorMessage(dto.errorMessage())
                .timestamp(dto.timestamp() != null ? dto.timestamp() : LocalDateTime.now())
                .build();
        return auditLogRepository.save(entity);
    }

    /**
     * Returns a single audit log by DB id.
     */
    public AuditEventDto getLogById(Long id) {
        AuditLog entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new AuditLogNotFoundException("Audit log not found with id: " + id));
        return toDto(entity);
    }

    // -----------------------------------------------------------------------
    // Private mapper
    // -----------------------------------------------------------------------
    private AuditEventDto toDto(AuditLog entity) {
        return new AuditEventDto(
                entity.getEventId(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getDetails(),
                entity.getIpAddress(),
                entity.getServiceName(),
                entity.isSuccess(),
                entity.getErrorMessage(),
                entity.getTimestamp()
        );
    }
}
