package com.seip.analytics.dto;

import java.math.BigDecimal;

public record DepartmentReportDto(
        Long departmentId,
        String departmentName,
        BigDecimal totalAmount,
        long employeeCount,
        BigDecimal avgPerEmployee
) {}
