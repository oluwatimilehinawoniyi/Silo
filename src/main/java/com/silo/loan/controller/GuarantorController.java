package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.LoanGuarantorResponse;
import com.silo.loan.service.LoanGuarantorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guarantors")
@Tag(name = "Guarantors", description = "Responding to guarantor invites")
public class GuarantorController {

    private final LoanGuarantorService loanGuarantorService;

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a guarantor invite")
    public ResponseEntity<ApiResponse<LoanGuarantorResponse>> accept(
            @Parameter(description = "Loan guarantor id") @PathVariable UUID id,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        LoanGuarantorResponse response = loanGuarantorService.accept(memberId, id);
        return ResponseEntity.ok(ApiResponse.success("Guarantor invite accepted", response));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline a guarantor invite")
    public ResponseEntity<ApiResponse<LoanGuarantorResponse>> decline(
            @Parameter(description = "Loan guarantor id") @PathVariable UUID id,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        LoanGuarantorResponse response = loanGuarantorService.decline(memberId, id);
        return ResponseEntity.ok(ApiResponse.success("Guarantor invite declined", response));
    }
}
