package com.seip.audit.service;

import com.seip.audit.dto.AuditEventDto;
import com.seip.audit.dto.PageResponse;
import com.seip.audit.entity.AuditLog;
import com.seip.audit.exception.AuditLogNotFoundException;
import com.seip.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private AuditLog sampleLog;
    private final String sampleEventId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        sampleLog = AuditLog.builder()
                .id(1L)
                .eventId(sampleEventId)
                .userId("user-123")
                .username("john.doe")
                .action("EXPENSE_SUBMITTED")
                .resourceType("EXPENSE")
                .resourceId("42")
                .details("{\"expenseId\":42, \"amount\":1500.00}")
                .ipAddress("10.0.0.1")
                .serviceName("expense-service")
                .success(true)
                .errorMessage(null)
                .timestamp(LocalDateTime.of(2024, 6, 15, 9, 0))
                .build();
    }

    // -----------------------------------------------------------------------
    // getLogs tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getLogs returns paginated results with no filters")
    void getLogs_returnsPaginatedResults_withNoFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by("timestamp").descending());
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        PageResponse<AuditEventDto> result = auditService.getLogs(pageable, null, null, null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).eventId()).isEqualTo(sampleEventId);
        assertThat(result.content().get(0).action()).isEqualTo("EXPENSE_SUBMITTED");

        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getLogs passes userId filter to repository")
    void getLogs_passesFilters_toRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2024, 12, 31, 23, 59);
        Page<AuditLog> emptyPage = Page.empty(pageable);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(emptyPage);

        // Act
        PageResponse<AuditEventDto> result = auditService.getLogs(pageable, "user-123", "EXPENSE", from, to);

        // Assert
        verify(auditLogRepository).findAll(any(Specification.class), eq(pageable));
        assertThat(result.totalElements()).isZero();
    }

    // -----------------------------------------------------------------------
    // getLogsByExpenseId tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getLogsByExpenseId returns list of matching audit logs")
    void getLogsByExpenseId_returnsList_whenLogsExist() {
        // Arrange
        when(auditLogRepository.findByResourceIdAndResourceType("42", "EXPENSE"))
                .thenReturn(List.of(sampleLog));

        // Act
        List<AuditEventDto> result = auditService.getLogsByExpenseId("42");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).resourceId()).isEqualTo("42");
        assertThat(result.get(0).resourceType()).isEqualTo("EXPENSE");
    }

    @Test
    @DisplayName("getLogsByExpenseId returns empty list when no logs found")
    void getLogsByExpenseId_returnsEmptyList_whenNotFound() {
        // Arrange
        when(auditLogRepository.findByResourceIdAndResourceType("999", "EXPENSE"))
                .thenReturn(List.of());

        // Act
        List<AuditEventDto> result = auditService.getLogsByExpenseId("999");

        // Assert
        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getLogsByUserId tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getLogsByUserId returns page for specific user")
    void getLogsByUserId_returnsPage_forUser() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1);
        when(auditLogRepository.findByUserId("user-123", pageable)).thenReturn(page);

        // Act
        PageResponse<AuditEventDto> result = auditService.getLogsByUserId("user-123", pageable);

        // Assert
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).userId()).isEqualTo("user-123");
        verify(auditLogRepository).findByUserId("user-123", pageable);
    }

    // -----------------------------------------------------------------------
    // saveAuditLog tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("saveAuditLog persists entity and returns saved log")
    void saveAuditLog_persistsAndReturns() {
        // Arrange
        AuditEventDto dto = new AuditEventDto(
                null, "user-456", "jane.doe", "EXPENSE_APPROVED",
                "EXPENSE", "77", "{\"amount\":2000}", "192.168.1.1",
                "expense-service", true, null,
                LocalDateTime.of(2024, 6, 16, 11, 0)
        );
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(sampleLog);

        // Act
        AuditLog saved = auditService.saveAuditLog(dto);

        // Assert
        assertThat(saved).isNotNull();
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo("user-456");
        assertThat(captured.getAction()).isEqualTo("EXPENSE_APPROVED");
        assertThat(captured.getResourceId()).isEqualTo("77");
        assertThat(captured.isSuccess()).isTrue();
        assertThat(captured.getEventId()).isNotBlank(); // auto-generated UUID
    }

    @Test
    @DisplayName("saveAuditLog uses provided eventId when not null")
    void saveAuditLog_usesProvidedEventId_whenPresent() {
        // Arrange
        String customEventId = "custom-event-id-12345";
        AuditEventDto dto = new AuditEventDto(
                customEventId, "user-789", null, "FRAUD_DETECTED",
                "FRAUD", "88", "{}", null, "fraud-service",
                false, "High risk", LocalDateTime.now()
        );
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuditLog saved = auditService.saveAuditLog(dto);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(customEventId);
    }

    // -----------------------------------------------------------------------
    // getLogById tests
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("getLogById returns DTO when log exists")
    void getLogById_returnsDto_whenFound() {
        // Arrange
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(sampleLog));

        // Act
        AuditEventDto result = auditService.getLogById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.eventId()).isEqualTo(sampleEventId);
        assertThat(result.username()).isEqualTo("john.doe");
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("getLogById throws AuditLogNotFoundException when not found")
    void getLogById_throwsException_whenNotFound() {
        // Arrange
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditService.getLogById(999L))
                .isInstanceOf(AuditLogNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("toDto maps all fields correctly")
    void getLogById_mapsAllFieldsCorrectly() {
        // Arrange
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(sampleLog));

        // Act
        AuditEventDto result = auditService.getLogById(1L);

        // Assert all fields
        assertThat(result.eventId()).isEqualTo(sampleEventId);
        assertThat(result.userId()).isEqualTo("user-123");
        assertThat(result.username()).isEqualTo("john.doe");
        assertThat(result.action()).isEqualTo("EXPENSE_SUBMITTED");
        assertThat(result.resourceType()).isEqualTo("EXPENSE");
        assertThat(result.resourceId()).isEqualTo("42");
        assertThat(result.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(result.serviceName()).isEqualTo("expense-service");
        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.timestamp()).isEqualTo(LocalDateTime.of(2024, 6, 15, 9, 0));
    }
}
