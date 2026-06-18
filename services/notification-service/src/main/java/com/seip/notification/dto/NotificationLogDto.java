package com.seip.notification.dto;

import com.seip.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDto {
    private Long id;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String body;
    private NotificationType notificationType;
    private String referenceId;
    private String status;
    private String errorMessage;
    private LocalDateTime sentAt;
}
