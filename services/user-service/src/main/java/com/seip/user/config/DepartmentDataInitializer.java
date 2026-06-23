package com.seip.user.config;

import com.seip.user.entity.Department;
import com.seip.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartmentDataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;

    @Override
    public void run(String... args) {
        if (departmentRepository.count() > 0) {
            return;
        }

        List<Department> defaults = List.of(
                department("Engineering", "ENG"),
                department("Marketing", "MKT"),
                department("Sales", "SLS"),
                department("Finance", "FIN"),
                department("Operations", "OPS"),
                department("Human Resources", "HR"),
                department("Legal", "LGL"),
                department("Product", "PRD"),
                department("Design", "DSN"),
                department("Customer Support", "CST")
        );

        departmentRepository.saveAll(defaults);
        log.info("Seeded {} default departments", defaults.size());
    }

    private Department department(String name, String code) {
        return Department.builder()
                .name(name)
                .code(code)
                .budget(BigDecimal.ZERO)
                .build();
    }
}
