package com.seip.user.repository;

import com.seip.user.entity.ManagerMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerMappingRepository extends JpaRepository<ManagerMapping, Long> {

    List<ManagerMapping> findByEmployeeId(Long employeeId);

    List<ManagerMapping> findByManagerId(Long managerId);

    Optional<ManagerMapping> findByEmployeeIdAndIsPrimary(Long employeeId, boolean isPrimary);

    boolean existsByEmployeeIdAndManagerId(Long employeeId, Long managerId);

    void deleteByEmployeeIdAndManagerId(Long employeeId, Long managerId);
}
