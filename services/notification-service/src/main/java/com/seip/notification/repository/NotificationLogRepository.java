package com.seip.notification.repository;

import com.seip.notification.entity.NotificationLog;
import com.seip.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findAll(Pageable pageable);

    Page<NotificationLog> findByRecipientEmail(String recipientEmail, Pageable pageable);

    Page<NotificationLog> findByNotificationType(NotificationType type, Pageable pageable);

    Page<NotificationLog> findByStatus(String status, Pageable pageable);

    List<NotificationLog> findByReferenceId(String referenceId);

    Page<NotificationLog> findBySentAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByStatus(String status);

    long countByNotificationType(NotificationType type);
}
