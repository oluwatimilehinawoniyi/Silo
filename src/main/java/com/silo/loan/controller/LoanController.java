package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.LoanDetailResponse;
import com.silo.loan.service.LoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loans")
@Tag(name = "Loans", description = "Viewing loan status, schedule, and outstanding balance")
public class LoanController {

    private final LoanQueryService loanQueryService;

    @GetMapping("/{id}")
    @Operation(summary = "View a loan's status, installment schedule, and outstanding balance")
    public ResponseEntity<ApiResponse<LoanDetailResponse>> getLoan(
            @Parameter(description = "Loan id") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(loanQueryService.getLoanDetail(id)));
    }
}
