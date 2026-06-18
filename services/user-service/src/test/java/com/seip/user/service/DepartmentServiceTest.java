package com.seip.user.service;

import com.seip.user.dto.DepartmentCreateRequest;
import com.seip.user.dto.DepartmentDto;
import com.seip.user.entity.Department;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.DepartmentMapper;
import com.seip.user.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService Unit Tests")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private DepartmentDto departmentDto;
    private DepartmentCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .budget(new BigDecimal("500000"))
                .build();

        departmentDto = DepartmentDto.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .budget(new BigDecimal("500000"))
                .build();

        createRequest = DepartmentCreateRequest.builder()
                .name("Engineering")
                .code("ENG")
                .budget(new BigDecimal("500000"))
                .build();
    }

    @Test
    @DisplayName("createDepartment - success when code is unique")
    void testCreateDepartment_success() {
        // Arrange
        when(departmentRepository.existsByCode(anyString())).thenReturn(false);
        when(departmentMapper.toEntity(any(DepartmentCreateRequest.class))).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(departmentDto);

        // Act
        DepartmentDto result = departmentService.createDepartment(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Engineering");
        assertThat(result.getCode()).isEqualTo("ENG");
        assertThat(result.getBudget()).isEqualByComparingTo(new BigDecimal("500000"));

        verify(departmentRepository).existsByCode("ENG");
        verify(departmentRepository).save(any(Department.class));
        verify(departmentMapper).toDto(department);
    }

    @Test
    @DisplayName("createDepartment - throws DuplicateResourceException when code exists")
    void testCreateDepartment_duplicateCode_throwsException() {
        // Arrange
        when(departmentRepository.existsByCode(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> departmentService.createDepartment(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("code")
                .hasMessageContaining("ENG");

        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    @Test
    @DisplayName("getAllDepartments - returns list of all departments")
    void testGetAllDepartments() {
        // Arrange
        Department dept2 = Department.builder()
                .id(2L)
                .name("Finance")
                .code("FIN")
                .budget(new BigDecimal("300000"))
                .build();

        DepartmentDto dto2 = DepartmentDto.builder()
                .id(2L)
                .name("Finance")
                .code("FIN")
                .budget(new BigDecimal("300000"))
                .build();

        when(departmentRepository.findAll()).thenReturn(Arrays.asList(department, dept2));
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);
        when(departmentMapper.toDto(dept2)).thenReturn(dto2);

        // Act
        List<DepartmentDto> result = departmentService.getAllDepartments();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCode()).isEqualTo("ENG");
        assertThat(result.get(1).getCode()).isEqualTo("FIN");

        verify(departmentRepository).findAll();
        verify(departmentMapper, times(2)).toDto(any(Department.class));
    }

    @Test
    @DisplayName("getDepartmentById - throws ResourceNotFoundException when not found")
    void testGetDepartmentById_notFound_throwsException() {
        // Arrange
        when(departmentRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> departmentService.getDepartmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department")
                .hasMessageContaining("99");

        verify(departmentMapper, never()).toDto(any(Department.class));
    }
}
