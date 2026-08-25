package com.silo.contribution.controller;

import com.silo.common.exception.ResourceNotFoundException;
import com.silo.common.response.ApiResponse;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.contribution.dto.AutoDebitMandateUpdateRequest;
import com.silo.contribution.service.ContributionAutoDebitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Setup (POST) lives in the paymentgateway module instead - it needs to
 * resolve the member's Paystack authorization code, and contribution can't
 * depend on paymentgateway without creating a module cycle (paymentgateway
 * already depends on contribution for the charge sweep).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contributions/auto-debit")
@Tag(name = "Auto-Debit", description = "Recurring contributions charged automatically via a saved Paystack card")
public class ContributionAutoDebitController {

    private final ContributionAutoDebitService autoDebitService;

    @GetMapping
    @Operation(summary = "View your current auto-debit mandate, if any")
    public ResponseEntity<ApiResponse<AutoDebitMandateSummary>> getCurrent(Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        AutoDebitMandateSummary response = autoDebitService.getCurrent(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("No auto-debit mandate found for this member"));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping
    @Operation(
            summary = "Change amount/periodicity, or pause/resume/cancel",
            description = "amount and periodicity are optional field updates; action (PAUSE, RESUME, CANCEL) is "
                    + "an optional state change - either or both can be sent in one call.")
    public ResponseEntity<ApiResponse<AutoDebitMandateSummary>> update(
            @Valid @RequestBody AutoDebitMandateUpdateRequest request,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        AutoDebitMandateSummary response = autoDebitService.update(memberId, request);
        return ResponseEntity.ok(ApiResponse.success("Auto-debit mandate updated successfully", response));
    }
}
