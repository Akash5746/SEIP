package com.seip.expense.mapper;

import com.seip.expense.dto.ExpenseCategoryDto;
import com.seip.expense.entity.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpenseCategoryMapper {

    ExpenseCategoryDto toDto(ExpenseCategory category);

    List<ExpenseCategoryDto> toDtoList(List<ExpenseCategory> categories);
}
