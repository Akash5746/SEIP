package com.seip.user.service;

import com.seip.user.dto.EmployeeCreateRequest;
import com.seip.user.dto.EmployeeDto;
import com.seip.user.dto.EmployeeUpdateRequest;
import com.seip.user.dto.PageResponse;
import com.seip.user.entity.Department;
import com.seip.user.entity.Employee;
import com.seip.user.exception.AccessDeniedException;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.EmployeeMapper;
import com.seip.user.repository.DepartmentRepository;
import com.seip.user.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;
    private final JdbcTemplate jdbcTemplate;

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
        return enrichEmployeeDto(employeeMapper.toDto(saved));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> getAllEmployees(Pageable pageable) {
        log.debug("Fetching all employees, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Employee> page = employeeRepository.findAll(pageable);
        List<EmployeeDto> content = page.getContent()
                .stream()
                .map(employeeMapper::toDto)
                .map(this::enrichEmployeeDto)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeById(Long id) {
        log.debug("Fetching employee with id: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return enrichEmployeeDto(employeeMapper.toDto(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeDto getEmployeeByAuthUserId(Long authUserId) {
        log.debug("Fetching employee with authUserId: {}", authUserId);
        Employee employee = employeeRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "authUserId", authUserId));
        return enrichEmployeeDto(employeeMapper.toDto(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeDto getAuthorizedEmployeeByAuthUserId(Long requesterAuthUserId, String requesterRole, Long targetAuthUserId) {
        Employee target = employeeRepository.findByAuthUserId(targetAuthUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "authUserId", targetAuthUserId));

        if (requesterAuthUserId.equals(targetAuthUserId) || isAdmin(requesterRole)) {
            return enrichEmployeeDto(employeeMapper.toDto(target));
        }

        if (!isManager(requesterRole)) {
            throw new AccessDeniedException("You do not have permission to view this employee");
        }

        Employee requester = getEmployeeEntityByAuthUserId(requesterAuthUserId);
        validateSameDepartment(requester, target);
        validateEmployeeRole(targetAuthUserId);
        return enrichEmployeeDto(employeeMapper.toDto(target));
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
        return enrichEmployeeDto(employeeMapper.toDto(updated));
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
                .map(this::enrichEmployeeDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> getDepartmentEmployeesForCurrentUser(Long requesterAuthUserId, String requesterRole) {
        if (isAdmin(requesterRole)) {
            Set<Long> employeeAuthUserIds = getActiveEmployeeAuthUserIds();
            return employeeRepository.findAll().stream()
                    .filter(Employee::isActive)
                    .filter(employee -> employee.getAuthUserId() != null && employeeAuthUserIds.contains(employee.getAuthUserId()))
                    .map(employeeMapper::toDto)
                    .map(this::enrichEmployeeDto)
                    .collect(Collectors.toList());
        }

        if (!isManager(requesterRole)) {
            throw new AccessDeniedException("Only managers can access department employees");
        }

        Employee requester = getEmployeeEntityByAuthUserId(requesterAuthUserId);
        if (requester.getDepartment() == null) {
            return List.of();
        }

        Set<Long> departmentEmployeeAuthUserIds = getActiveEmployeeAuthUserIdsByDepartment(requester.getDepartment().getId());
        return employeeRepository.findByDepartmentIdAndActiveTrue(requester.getDepartment().getId()).stream()
                .filter(employee -> employee.getAuthUserId() != null)
                .filter(employee -> departmentEmployeeAuthUserIds.contains(employee.getAuthUserId()))
                .filter(employee -> !employee.getAuthUserId().equals(requesterAuthUserId))
                .map(employeeMapper::toDto)
                .map(this::enrichEmployeeDto)
                .collect(Collectors.toList());
    }

    private Set<Long> getActiveEmployeeAuthUserIds() {
        return new HashSet<>(jdbcTemplate.queryForList("""
                SELECT e.auth_user_id
                FROM users.employees e
                JOIN auth.users u ON u.id = e.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = u.id
                JOIN auth.roles r ON r.id = ur.role_id
                WHERE e.is_active = true
                  AND e.auth_user_id IS NOT NULL
                  AND u.enabled = true
                  AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                """, Long.class));
    }

    private Set<Long> getActiveEmployeeAuthUserIdsByDepartment(Long departmentId) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                SELECT e.auth_user_id
                FROM users.employees e
                JOIN auth.users u ON u.id = e.auth_user_id
                JOIN auth.user_roles ur ON ur.user_id = u.id
                JOIN auth.roles r ON r.id = ur.role_id
                WHERE e.department_id = ?
                  AND e.is_active = true
                  AND e.auth_user_id IS NOT NULL
                  AND u.enabled = true
                  AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                """, Long.class, departmentId));
    }

    private EmployeeDto enrichEmployeeDto(EmployeeDto employeeDto) {
        if (employeeDto.getAuthUserId() == null) {
            return employeeDto;
        }

        jdbcTemplate.query("""
                SELECT u.username, r.name AS role
                FROM auth.users u
                JOIN auth.user_roles ur ON ur.user_id = u.id
                JOIN auth.roles r ON r.id = ur.role_id
                WHERE u.id = ?
                LIMIT 1
                """, rs -> {
            if (rs.next()) {
                employeeDto.setUsername(rs.getString("username"));
                employeeDto.setRole(rs.getString("role"));
            }
            return null;
        }, employeeDto.getAuthUserId());

        return employeeDto;
    }

    private Employee getEmployeeEntityByAuthUserId(Long authUserId) {
        return employeeRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "authUserId", authUserId));
    }

    private void validateSameDepartment(Employee requester, Employee target) {
        if (requester.getDepartment() == null || target.getDepartment() == null) {
            throw new AccessDeniedException("Department membership is required for this action");
        }

        if (!requester.getDepartment().getId().equals(target.getDepartment().getId())) {
            throw new AccessDeniedException("You can only access employees within your department");
        }
    }

    private void validateEmployeeRole(Long authUserId) {
        Boolean isEmployee = jdbcTemplate.query("""
                SELECT CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM auth.users u
                        JOIN auth.user_roles ur ON ur.user_id = u.id
                        JOIN auth.roles r ON r.id = ur.role_id
                        WHERE u.id = ?
                          AND u.enabled = true
                          AND UPPER(r.name) = 'ROLE_EMPLOYEE'
                    ) THEN TRUE
                    ELSE FALSE
                END
                """, rs -> rs.next() ? rs.getBoolean(1) : Boolean.FALSE, authUserId);

        if (!Boolean.TRUE.equals(isEmployee)) {
            throw new AccessDeniedException("Managers can only access employee accounts");
        }
    }

    private boolean isManager(String requesterRole) {
        return normalizeRole(requesterRole).equals("ROLE_MANAGER");
    }

    private boolean isAdmin(String requesterRole) {
        return normalizeRole(requesterRole).equals("ROLE_ADMIN");
    }

    private String normalizeRole(String requesterRole) {
        if (requesterRole == null || requesterRole.isBlank()) {
            return "";
        }

        String normalized = requesterRole.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
