package com.seip.user.service;

import com.seip.user.dto.EmployeeCreateRequest;
import com.seip.user.dto.EmployeeDto;
import com.seip.user.dto.EmployeeUpdateRequest;
import com.seip.user.entity.Department;
import com.seip.user.entity.Employee;
import com.seip.user.exception.DuplicateResourceException;
import com.seip.user.exception.ResourceNotFoundException;
import com.seip.user.mapper.EmployeeMapper;
import com.seip.user.repository.DepartmentRepository;
import com.seip.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private EmployeeDto employeeDto;
    private EmployeeCreateRequest createRequest;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .code("ENG")
                .build();

        employee = Employee.builder()
                .id(1L)
                .authUserId(100L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@seip.com")
                .phone("+91-9999999999")
                .department(department)
                .designation("Software Engineer")
                .monthlyExpenseLimit(new BigDecimal("50000"))
                .active(true)
                .build();

        employeeDto = EmployeeDto.builder()
                .id(1L)
                .authUserId(100L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@seip.com")
                .departmentId(1L)
                .departmentName("Engineering")
                .designation("Software Engineer")
                .monthlyExpenseLimit(new BigDecimal("50000"))
                .active(true)
                .build();

        createRequest = EmployeeCreateRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@seip.com")
                .phone("+91-9999999999")
                .departmentId(1L)
                .designation("Software Engineer")
                .authUserId(100L)
                .employeeCode("EMP001")
                .build();
    }

    @Test
    @DisplayName("createEmployee - success when email and code are unique")
    void testCreateEmployee_success() {
        // Arrange
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode(anyString())).thenReturn(false);
        when(employeeMapper.toEntity(any(EmployeeCreateRequest.class))).thenReturn(employee);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toDto(any(Employee.class))).thenReturn(employeeDto);

        // Act
        EmployeeDto result = employeeService.createEmployee(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@seip.com");
        assertThat(result.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(result.getFirstName()).isEqualTo("John");

        verify(employeeRepository).existsByEmail("john.doe@seip.com");
        verify(employeeRepository).existsByEmployeeCode("EMP001");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("createEmployee - throws DuplicateResourceException when email exists")
    void testCreateEmployee_duplicateEmail_throwsException() {
        // Arrange
        when(employeeRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> employeeService.createEmployee(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("getEmployeeById - throws ResourceNotFoundException when employee does not exist")
    void testGetEmployeeById_notFound_throwsException() {
        // Arrange
        when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee")
                .hasMessageContaining("99");

        verify(employeeMapper, never()).toDto(any(Employee.class));
    }

    @Test
    @DisplayName("updateEmployee - success when employee exists")
    void testUpdateEmployee_success() {
        // Arrange
        EmployeeUpdateRequest updateRequest = EmployeeUpdateRequest.builder()
                .firstName("Jane")
                .phone("+91-8888888888")
                .monthlyExpenseLimit(new BigDecimal("75000"))
                .build();

        Employee updatedEmployee = Employee.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@seip.com")
                .phone("+91-8888888888")
                .monthlyExpenseLimit(new BigDecimal("75000"))
                .active(true)
                .build();

        EmployeeDto updatedDto = EmployeeDto.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@seip.com")
                .monthlyExpenseLimit(new BigDecimal("75000"))
                .active(true)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(employeeMapper.toDto(any(Employee.class))).thenReturn(updatedDto);

        // Act
        EmployeeDto result = employeeService.updateEmployee(1L, updateRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getMonthlyExpenseLimit()).isEqualByComparingTo(new BigDecimal("75000"));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("deactivateEmployee - sets isActive to false")
    void testDeactivateEmployee() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        // Act
        employeeService.deactivateEmployee(1L);

        // Assert
        assertThat(employee.isActive()).isFalse();
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(employee);
    }
}
