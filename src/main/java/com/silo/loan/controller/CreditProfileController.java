package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.BorrowerRiskProfileResponse;
import com.silo.loan.dto.GuarantorCredibilityProfileResponse;
import com.silo.loan.service.BorrowerRiskService;
import com.silo.loan.service.GuarantorCredibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit-profiles")
@Tag(name = "Credit Profiles", description = "Borrower risk and guarantor credibility scoring")
public class CreditProfileController {

    private final BorrowerRiskService borrowerRiskService;
    private final GuarantorCredibilityService guarantorCredibilityService;

    @GetMapping("/borrower-risk/{memberId}")
    @PreAuthorize("hasRole('OFFICER') or #memberId.toString() == authentication.name")
    @Operation(summary = "View a member's borrower risk profile",
            description = "Officers can view any member's profile. Members can only view their own.")
    public ResponseEntity<ApiResponse<BorrowerRiskProfileResponse>> getBorrowerRiskProfile(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(borrowerRiskService.getProfile(memberId)));
    }

    @GetMapping("/guarantor-credibility/{memberId}")
    @PreAuthorize("hasRole('OFFICER') or #memberId.toString() == authentication.name")
    @Operation(summary = "View a member's guarantor credibility profile",
            description = "Officers can view any member's profile. Members can only view their own.")
    public ResponseEntity<ApiResponse<GuarantorCredibilityProfileResponse>> getGuarantorCredibilityProfile(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(guarantorCredibilityService.getProfile(memberId)));
    }
}
