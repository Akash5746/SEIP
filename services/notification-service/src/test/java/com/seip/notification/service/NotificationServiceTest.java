package com.seip.notification.service;

import com.seip.notification.dto.NotificationLogDto;
import com.seip.notification.entity.NotificationLog;
import com.seip.notification.entity.NotificationType;
import com.seip.notification.exception.NotificationNotFoundException;
import com.seip.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = NotificationLog.builder()
                .id(1L)
                .recipientEmail("manager@seip.com")
                .recipientName("Test Manager")
                .subject("Test Expense Submitted")
                .body("<html>Test Body</html>")
                .notificationType(NotificationType.EXPENSE_SUBMITTED)
                .referenceId("42")
                .status("SENT")
                .errorMessage(null)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // getAllLogs tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getAllLogs returns paginated DTOs successfully")
    void getAllLogs_returnsPage_whenLogsExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
        Page<NotificationLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1);
        when(notificationLogRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<NotificationLogDto> result = notificationService.getAllLogs(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        NotificationLogDto dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getRecipientEmail()).isEqualTo("manager@seip.com");
        assertThat(dto.getNotificationType()).isEqualTo(NotificationType.EXPENSE_SUBMITTED);
        assertThat(dto.getStatus()).isEqualTo("SENT");

        verify(notificationLogRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("getAllLogs returns empty page when no logs exist")
    void getAllLogs_returnsEmptyPage_whenNoLogsExist() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<NotificationLog> emptyPage = Page.empty(pageable);
        when(notificationLogRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<NotificationLogDto> result = notificationService.getAllLogs(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getLogById tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getLogById returns DTO when log exists")
    void getLogById_returnsDto_whenLogFound() {
        // Arrange
        when(notificationLogRepository.findById(1L)).thenReturn(Optional.of(sampleLog));

        // Act
        NotificationLogDto result = notificationService.getLogById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRecipientEmail()).isEqualTo("manager@seip.com");
        assertThat(result.getRecipientName()).isEqualTo("Test Manager");
        assertThat(result.getSubject()).isEqualTo("Test Expense Submitted");
        assertThat(result.getReferenceId()).isEqualTo("42");

        verify(notificationLogRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getLogById throws NotificationNotFoundException when log not found")
    void getLogById_throwsException_whenLogNotFound() {
        // Arrange
        when(notificationLogRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> notificationService.getLogById(999L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessageContaining("999");

        verify(notificationLogRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getLogById maps all fields correctly")
    void getLogById_mapsAllFields_correctly() {
        // Arrange
        NotificationLog log = NotificationLog.builder()
                .id(5L)
                .recipientEmail("employee@seip.com")
                .recipientName("John Doe")
                .subject("Expense Approved")
                .body("<html>Approved</html>")
                .notificationType(NotificationType.EXPENSE_APPROVED)
                .referenceId("100")
                .status("SENT")
                .errorMessage(null)
                .sentAt(LocalDateTime.of(2024, 6, 15, 10, 30))
                .build();

        when(notificationLogRepository.findById(5L)).thenReturn(Optional.of(log));

        // Act
        NotificationLogDto result = notificationService.getLogById(5L);

        // Assert
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getRecipientEmail()).isEqualTo("employee@seip.com");
        assertThat(result.getRecipientName()).isEqualTo("John Doe");
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.EXPENSE_APPROVED);
        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("getLogById maps FAILED status and error message correctly")
    void getLogById_mapsFailedStatus_correctly() {
        // Arrange
        NotificationLog failedLog = NotificationLog.builder()
                .id(7L)
                .recipientEmail("admin@seip.com")
                .notificationType(NotificationType.FRAUD_ALERT)
                .referenceId("55")
                .status("FAILED")
                .errorMessage("SMTP connection refused")
                .sentAt(LocalDateTime.now())
                .build();

        when(notificationLogRepository.findById(7L)).thenReturn(Optional.of(failedLog));

        // Act
        NotificationLogDto result = notificationService.getLogById(7L);

        // Assert
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).isEqualTo("SMTP connection refused");
    }

    @Test
    @DisplayName("getAllLogs calls repository with exact pageable")
    void getAllLogs_passesPageableToRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(2, 5, Sort.by("sentAt"));
        when(notificationLogRepository.findAll(pageable)).thenReturn(Page.empty());

        // Act
        notificationService.getAllLogs(pageable);

        // Assert
        verify(notificationLogRepository, times(1)).findAll(pageable);
        verifyNoMoreInteractions(notificationLogRepository);
    }
}
