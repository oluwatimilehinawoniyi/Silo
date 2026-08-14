package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanRequestSubmitRequest;
import com.silo.loan.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-requests")
@Tag(name = "Loan Requests", description = "Submitting loan requests")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    @PostMapping
    @Operation(
            summary = "Submit a loan request",
            description = "Member must be ACTIVE and KYC_VERIFIED. Submits on behalf of the authenticated member.")
    public ResponseEntity<ApiResponse<LoanRequestResponse>> submit(
            @Valid @RequestBody LoanRequestSubmitRequest request,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        LoanRequestResponse response = loanRequestService.submit(memberId, request);
        return ResponseEntity.ok(ApiResponse.success("Loan request submitted successfully", response));
    }
}
