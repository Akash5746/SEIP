package com.seip.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateRequest {

    private String firstName;
    private String lastName;
    private String phone;
    private Long departmentId;
    private String designation;
    private BigDecimal monthlyExpenseLimit;
}
