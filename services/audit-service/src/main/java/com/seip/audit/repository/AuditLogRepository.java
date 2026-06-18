package com.seip.audit.repository;

import com.seip.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByEventId(String eventId);

    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    Page<AuditLog> findByResourceId(String resourceId, Pageable pageable);

    Page<AuditLog> findByActionContaining(String action, Pageable pageable);

    List<AuditLog> findByResourceIdAndResourceType(String resourceId, String resourceType);

    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<AuditLog> findBySuccess(boolean success, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :from AND :to")
    Page<AuditLog> findByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:action IS NULL OR a.action LIKE %:action%) AND " +
           "(:from IS NULL OR a.timestamp >= :from) AND " +
           "(:to IS NULL OR a.timestamp <= :to)")
    Page<AuditLog> findWithFilters(
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    long countByAction(String action);

    long countBySuccess(boolean success);
}
