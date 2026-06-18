package com.seip.user.service;

import com.seip.user.dto.DepartmentCreateRequest;
import com.seip.user.dto.DepartmentDto;
import com.seip.user.entity.Department;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.DepartmentMapper;
import com.seip.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional
    public DepartmentDto createDepartment(DepartmentCreateRequest request) {
        log.info("Creating department with code: {}", request.getCode());

        if (departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department", "code", request.getCode());
        }

        Department department = departmentMapper.toEntity(request);
        Department saved = departmentRepository.save(department);

        log.info("Department created successfully with id: {}", saved.getId());
        return departmentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        log.debug("Fetching all departments");
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        log.debug("Fetching department with id: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return departmentMapper.toDto(department);
    }

    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentCreateRequest request) {
        log.info("Updating department with id: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        // Check for code collision (allow same code on same entity)
        if (!department.getCode().equals(request.getCode())
                && departmentRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Department", "code", request.getCode());
        }

        departmentMapper.updateEntity(request, department);
        Department updated = departmentRepository.save(department);

        log.info("Department updated successfully with id: {}", updated.getId());
        return departmentMapper.toDto(updated);
    }
}
