package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.AddGuarantorRequest;
import com.silo.loan.dto.LoanGuarantorResponse;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanRequestSubmitRequest;
import com.silo.loan.service.LoanGuarantorService;
import com.silo.loan.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-requests")
@Tag(name = "Loan Requests", description = "Submitting loan requests and inviting guarantors")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final LoanGuarantorService loanGuarantorService;

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

    @PostMapping("/{id}/guarantors")
    @Operation(
            summary = "Invite a guarantor for a loan request",
            description = "Only the requesting member can invite guarantors, while the request is PENDING.")
    public ResponseEntity<ApiResponse<LoanGuarantorResponse>> addGuarantor(
            @Parameter(description = "Loan request id") @PathVariable UUID id,
            @Valid @RequestBody AddGuarantorRequest request,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        LoanGuarantorResponse response = loanGuarantorService.addGuarantor(memberId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Guarantor invited successfully", response));
    }
}
