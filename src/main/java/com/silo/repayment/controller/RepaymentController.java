package com.silo.repayment.controller;

import com.silo.common.response.ApiResponse;
import com.silo.repayment.dto.LiabilityRepaymentRequest;
import com.silo.repayment.dto.RepaymentRequest;
import com.silo.repayment.dto.RepaymentResponse;
import com.silo.repayment.service.RepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/repayments")
@Tag(name = "Repayments", description = "Recording and viewing loan repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;

    @PostMapping
    @Operation(
            summary = "Record a borrower repayment",
            description = "Loan must be ACTIVE; only the loan's own borrower can repay it.")
    public ResponseEntity<ApiResponse<RepaymentResponse>> recordRepayment(
            @Valid @RequestBody RepaymentRequest request,
            Authentication authentication) {
        UUID payerMemberId = UUID.fromString(authentication.getName());
        RepaymentResponse response = repaymentService.recordBorrowerRepayment(payerMemberId, request);
        return ResponseEntity.ok(ApiResponse.success("Repayment recorded successfully", response));
    }

    @PostMapping("/liability")
    @Operation(
            summary = "Record a guarantor liability repayment",
            description = "Liability must be PENDING; only the assigned guarantor can pay it down.")
    public ResponseEntity<ApiResponse<RepaymentResponse>> recordLiabilityRepayment(
            @Valid @RequestBody LiabilityRepaymentRequest request,
            Authentication authentication) {
        UUID payerMemberId = UUID.fromString(authentication.getName());
        RepaymentResponse response = repaymentService.recordLiabilityRepayment(payerMemberId, request);
        return ResponseEntity.ok(ApiResponse.success("Liability repayment recorded successfully", response));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "View repayment history for a loan")
    public ResponseEntity<ApiResponse<List<RepaymentResponse>>> getHistory(
            @Parameter(description = "Loan id") @PathVariable UUID loanId) {
        return ResponseEntity.ok(ApiResponse.success(repaymentService.getHistory(loanId)));
    }
}
