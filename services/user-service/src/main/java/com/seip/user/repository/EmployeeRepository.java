package com.seip.user.repository;

import com.seip.user.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByAuthUserId(Long authUserId);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByDepartmentId(Long departmentId);

    List<Employee> findByDepartmentIdAndActiveTrue(Long departmentId);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    Page<Employee> findAll(Pageable pageable);
}
