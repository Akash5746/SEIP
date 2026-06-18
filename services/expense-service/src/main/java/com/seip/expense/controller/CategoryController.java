package com.seip.expense.controller;

import com.seip.expense.dto.ApiResponse;
import com.seip.expense.dto.ExpenseCategoryDto;
import com.seip.expense.service.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "APIs for expense category lookup")
public class CategoryController {

    private final ExpenseCategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active expense categories")
    public ResponseEntity<ApiResponse<List<ExpenseCategoryDto>>> getAllCategories() {
        List<ExpenseCategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense category by ID")
    public ResponseEntity<ApiResponse<ExpenseCategoryDto>> getCategoryById(@PathVariable Long id) {
        ExpenseCategoryDto dto = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
