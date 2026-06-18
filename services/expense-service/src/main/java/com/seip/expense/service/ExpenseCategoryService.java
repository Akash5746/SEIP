package com.seip.expense.service;

import com.seip.expense.dto.ExpenseCategoryDto;
import com.seip.expense.entity.ExpenseCategory;
import com.seip.expense.exception.ResourceNotFoundException;
import com.seip.expense.mapper.ExpenseCategoryMapper;
import com.seip.expense.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseCategoryMapper categoryMapper;

    public List<ExpenseCategoryDto> getAllCategories() {
        log.debug("Fetching all active expense categories");
        List<ExpenseCategory> categories = categoryRepository.findByIsActiveTrue();
        return categoryMapper.toDtoList(categories);
    }

    public ExpenseCategoryDto getCategoryById(Long id) {
        log.debug("Fetching expense category with id: {}", id);
        ExpenseCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
        return categoryMapper.toDto(category);
    }

    public ExpenseCategory findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", id));
    }
}
