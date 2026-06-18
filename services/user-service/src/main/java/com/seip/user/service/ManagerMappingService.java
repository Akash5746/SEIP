package com.seip.user.service;

import com.seip.user.dto.EmployeeDto;
import com.seip.user.entity.Employee;
import com.seip.user.entity.ManagerMapping;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.EmployeeMapper;
import com.seip.user.repository.EmployeeRepository;
import com.seip.user.repository.ManagerMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerMappingService {

    private final ManagerMappingRepository managerMappingRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public void assignManager(Long employeeId, Long managerId) {
        log.info("Assigning manager {} to employee {}", managerId, employeeId);

        if (employeeId.equals(managerId)) {
            throw new IllegalArgumentException("An employee cannot be their own manager");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager employee", "id", managerId));

        if (managerMappingRepository.existsByEmployeeIdAndManagerId(employeeId, managerId)) {
            throw new DuplicateResourceException(
                    "Manager mapping already exists for employee " + employeeId + " and manager " + managerId);
        }

        // If no primary assignment exists yet, make this one primary
        boolean noPrimaryExists = managerMappingRepository
                .findByEmployeeIdAndIsPrimary(employeeId, true)
                .isEmpty();

        ManagerMapping mapping = ManagerMapping.builder()
                .employee(employee)
                .manager(manager)
                .isPrimary(noPrimaryExists)
                .assignedAt(LocalDateTime.now())
                .build();

        managerMappingRepository.save(mapping);
        log.info("Manager assignment created successfully");
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getDirectReports(Long managerId) {
        log.debug("Fetching direct reports for manager: {}", managerId);

        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager employee", "id", managerId);
        }

        return managerMappingRepository.findByManagerId(managerId)
                .stream()
                .map(ManagerMapping::getEmployee)
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getManagerForEmployee(Long employeeId) {
        log.debug("Fetching manager for employee: {}", employeeId);

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }

        ManagerMapping mapping = managerMappingRepository
                .findByEmployeeIdAndIsPrimary(employeeId, true)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No primary manager found for employee with id: " + employeeId));

        return employeeMapper.toDto(mapping.getManager());
    }

    @Transactional
    public void removeManagerAssignment(Long employeeId, Long managerId) {
        log.info("Removing manager {} from employee {}", managerId, employeeId);

        if (!managerMappingRepository.existsByEmployeeIdAndManagerId(employeeId, managerId)) {
            throw new ResourceNotFoundException(
                    "Manager mapping not found for employee " + employeeId + " and manager " + managerId);
        }

        managerMappingRepository.deleteByEmployeeIdAndManagerId(employeeId, managerId);
        log.info("Manager assignment removed successfully");
    }
}
