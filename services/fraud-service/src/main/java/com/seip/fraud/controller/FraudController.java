package com.seip.fraud.controller;

import com.seip.fraud.dto.FraudAnalysisDto;
import com.seip.fraud.dto.FraudCheckRequestEvent;
import com.seip.fraud.dto.FraudDashboardDto;
import com.seip.fraud.dto.PageResponse;
import com.seip.fraud.entity.FraudAnalysis;
import com.seip.fraud.exception.ApiResponse;
import com.seip.fraud.service.FraudAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud Detection", description = "APIs for fraud analysis and risk management")
public class FraudController {

    private final FraudAnalysisService fraudAnalysisService;

    // -------------------------------------------------------------------------
    // POST /fraud/analyze  — manual trigger (also used for testing / backfill)
    // -------------------------------------------------------------------------

    @PostMapping("/analyze")
    @Operation(summary = "Manually trigger fraud analysis for an expense",
               description = "Accepts the same payload as the Kafka event. Useful for manual review or re-analysis.")
    public ResponseEntity<ApiResponse<FraudAnalysisDto>> analyzeExpense(
            @Valid @RequestBody FraudCheckRequestEvent request) {
        log.info("Manual fraud analysis triggered for expenseId={}", request.getExpenseId());

        FraudAnalysis analysis = fraudAnalysisService.analyzeExpense(request);

        FraudAnalysisDto dto = fraudAnalysisService.getAnalysisByExpenseId(analysis.getExpenseId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fraud analysis completed successfully", dto));
    }

    // -------------------------------------------------------------------------
    // GET /fraud/analysis/{expenseId}
    // -------------------------------------------------------------------------

    @GetMapping("/analysis/{expenseId}")
    @Operation(summary = "Retrieve fraud analysis for a specific expense")
    public ResponseEntity<ApiResponse<FraudAnalysisDto>> getAnalysis(
            @Parameter(description = "Expense ID to retrieve analysis for")
            @PathVariable Long expenseId) {
        FraudAnalysisDto dto = fraudAnalysisService.getAnalysisByExpenseId(expenseId);
        return ResponseEntity.ok(ApiResponse.success("Fraud analysis retrieved", dto));
    }

    // -------------------------------------------------------------------------
    // GET /fraud/dashboard
    // -------------------------------------------------------------------------

    @GetMapping("/dashboard")
    @Operation(summary = "Get fraud statistics dashboard",
               description = "Returns aggregated counts of analyzed expenses by risk level, duplicate count, and fraud rate.")
    public ResponseEntity<ApiResponse<FraudDashboardDto>> getDashboard() {
        FraudDashboardDto dashboard = fraudAnalysisService.getFraudDashboard();
        return ResponseEntity.ok(ApiResponse.success("Fraud dashboard retrieved", dashboard));
    }

    // -------------------------------------------------------------------------
    // GET /fraud/high-risk?page=0&size=20
    // -------------------------------------------------------------------------

    @GetMapping("/high-risk")
    @Operation(summary = "List high-risk expenses (paginated)",
               description = "Returns paginated list of expenses classified as HIGH risk.")
    public ResponseEntity<ApiResponse<PageResponse<FraudAnalysisDto>>> getHighRiskExpenses(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size")             @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "analysisTime"));
        Page<FraudAnalysisDto> resultPage = fraudAnalysisService.getHighRiskExpenses(pageable);

        PageResponse<FraudAnalysisDto> pageResponse = PageResponse.<FraudAnalysisDto>builder()
                .content(resultPage.getContent())
                .pageNumber(resultPage.getNumber())
                .pageSize(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .first(resultPage.isFirst())
                .last(resultPage.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.success("High risk expenses retrieved", pageResponse));
    }
}
