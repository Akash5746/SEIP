package com.seip.user.controller;

import com.seip.user.dto.ApiResponse;
import com.seip.user.dto.EmployeeDto;
import com.seip.user.dto.ManagerAssignRequest;
import com.seip.user.service.ManagerMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/managers")
@RequiredArgsConstructor
@Tag(name = "Manager Mappings", description = "Manager assignment and hierarchy endpoints")
public class ManagerController {

    private final ManagerMappingService managerMappingService;

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a manager to an employee")
    public ResponseEntity<ApiResponse<Void>> assignManager(
            @Valid @RequestBody ManagerAssignRequest request) {
        managerMappingService.assignManager(request.getEmployeeId(), request.getManagerId());
        return ResponseEntity.ok(ApiResponse.success("Manager assigned successfully"));
    }

    @GetMapping("/{managerId}/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get direct reports for a manager")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> getDirectReports(
            @PathVariable Long managerId) {
        List<EmployeeDto> reports = managerMappingService.getDirectReports(managerId);
        return ResponseEntity.ok(ApiResponse.success("Direct reports retrieved successfully", reports));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get primary manager for an employee")
    public ResponseEntity<ApiResponse<EmployeeDto>> getManagerForEmployee(
            @PathVariable Long employeeId) {
        EmployeeDto manager = managerMappingService.getManagerForEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Manager retrieved successfully", manager));
    }

    @DeleteMapping("/employee/{employeeId}/manager/{managerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove manager assignment from an employee")
    public ResponseEntity<ApiResponse<Void>> removeManagerAssignment(
            @PathVariable Long employeeId,
            @PathVariable Long managerId) {
        managerMappingService.removeManagerAssignment(employeeId, managerId);
        return ResponseEntity.ok(ApiResponse.success("Manager assignment removed successfully"));
    }
}
