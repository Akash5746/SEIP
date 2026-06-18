package com.seip.user.service;

import com.seip.user.dto.EmployeeCreateRequest;
import com.seip.user.dto.EmployeeDto;
import com.seip.user.dto.EmployeeUpdateRequest;
import com.seip.user.dto.PageResponse;
import com.seip.user.entity.Department;
import com.seip.user.entity.Employee;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.EmployeeMapper;
import com.seip.user.repository.DepartmentRepository;
import com.seip.user.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional
    public EmployeeDto createEmployee(EmployeeCreateRequest request) {
        log.info("Creating employee with email: {}", request.getEmail());

        if (request.getEmail() != null && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", request.getEmail());
        }

        if (request.getEmployeeCode() != null
                && employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee", "employeeCode", request.getEmployeeCode());
        }

        Employee employee = employeeMapper.toEntity(request);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee created successfully with id: {}", saved.getId());
        return employeeMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> getAllEmployees(Pageable pageable) {
        log.debug("Fetching all employees, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Employee> page = employeeRepository.findAll(pageable);
        List<EmployeeDto> content = page.getContent()
                .stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return employeeMapper.toDto(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeByAuthUserId(Long authUserId) {
        log.debug("Fetching employee with authUserId: {}", authUserId);
        Employee employee = employeeRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "authUserId", authUserId));
        return employeeMapper.toDto(employee);
    }

    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeUpdateRequest request) {
        log.info("Updating employee with id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }
        if (request.getMonthlyExpenseLimit() != null) {
            employee.setMonthlyExpenseLimit(request.getMonthlyExpenseLimit());
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }

        Employee updated = employeeRepository.save(employee);
        log.info("Employee updated successfully with id: {}", updated.getId());
        return employeeMapper.toDto(updated);
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        log.info("Deactivating employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        employee.setActive(false);
        employeeRepository.save(employee);
        log.info("Employee deactivated successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getEmployeesByDepartment(Long departmentId) {
        log.debug("Fetching employees for department: {}", departmentId);
        if (!departmentRepository.existsById(departmentId)) {
            throw new ResourceNotFoundException("Department", "id", departmentId);
        }
        return employeeRepository.findByDepartmentId(departmentId)
                .stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }
}
