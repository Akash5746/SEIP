package com.seip.notification.service;

import com.seip.notification.dto.NotificationLogDto;
import com.seip.notification.entity.NotificationLog;
import com.seip.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public Page<NotificationLogDto> getAllLogs(Pageable pageable) {
        log.debug("Fetching notification logs, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return notificationLogRepository.findAll(pageable).map(this::toDto);
    }

    public NotificationLogDto getLogById(Long id) {
        log.debug("Fetching notification log by id={}", id);
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new com.seip.notification.exception.NotificationNotFoundException(
                        "Notification log not found with id: " + id));
        return toDto(log);
    }

    private NotificationLogDto toDto(NotificationLog entity) {
        return NotificationLogDto.builder()
                .id(entity.getId())
                .recipientEmail(entity.getRecipientEmail())
                .recipientName(entity.getRecipientName())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .notificationType(entity.getNotificationType())
                .referenceId(entity.getReferenceId())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .build();
    }
}
