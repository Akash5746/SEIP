package com.seip.user.controller;

import com.seip.user.dto.ApiResponse;
import com.seip.user.dto.EmployeeCreateRequest;
import com.seip.user.dto.EmployeeDto;
import com.seip.user.dto.EmployeeUpdateRequest;
import com.seip.user.dto.PageResponse;
import com.seip.user.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all employees (paginated) - ADMIN only")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeDto>>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<EmployeeDto> employees = employeeService.getAllEmployees(pageable);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", employees));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get employee by ID - ADMIN only")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeById(@PathVariable Long id) {
        EmployeeDto employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", employee));
    }

    @GetMapping("/me")
    @Operation(summary = "Get own employee profile using gateway auth header")
    public ResponseEntity<ApiResponse<EmployeeDto>> getMyProfile(
            @RequestHeader("X-Auth-User-Id") Long authUserId) {
        EmployeeDto employee = employeeService.getEmployeeByAuthUserId(authUserId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", employee));
    }

    @GetMapping("/auth/{authUserId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get an employee by auth user ID with authorization checks")
    public ResponseEntity<ApiResponse<EmployeeDto>> getEmployeeByAuthUserId(
            @PathVariable Long authUserId,
            @RequestHeader("X-Auth-User-Id") Long requesterAuthUserId,
            @RequestHeader("X-Auth-User-Role") String requesterRole) {
        EmployeeDto employee = employeeService.getAuthorizedEmployeeByAuthUserId(
                requesterAuthUserId,
                requesterRole,
                authUserId
        );
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", employee));
    }

    @GetMapping("/my-department")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Get employees visible to the current manager/admin")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getMyDepartmentEmployees(
            @RequestHeader("X-Auth-User-Id") Long requesterAuthUserId,
            @RequestHeader("X-Auth-User-Role") String requesterRole) {
        List<EmployeeDto> employees = employeeService.getDepartmentEmployeesForCurrentUser(requesterAuthUserId, requesterRole);
        return ResponseEntity.ok(ApiResponse.success("Department employees retrieved successfully", employees));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new employee (ADMIN only)")
    public ResponseEntity<ApiResponse<EmployeeDto>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeDto created = employeeService.createEmployee(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an employee - ADMIN only")
    public ResponseEntity<ApiResponse<EmployeeDto>> updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeUpdateRequest request) {
        EmployeeDto updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an employee (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully"));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all employees in a department")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getEmployeesByDepartment(
            @PathVariable Long departmentId) {
        List<EmployeeDto> employees = employeeService.getEmployeesByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", employees));
    }
}
