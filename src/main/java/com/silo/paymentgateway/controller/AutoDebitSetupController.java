package com.silo.paymentgateway.controller;

import com.silo.common.response.ApiResponse;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.paymentgateway.dto.AutoDebitSetupRequest;
import com.silo.paymentgateway.service.AutoDebitSetupService;
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

/**
 * Handles POST only - GET/PATCH for an existing mandate live in the
 * contribution module's own controller. Setup has to live here because it
 * needs the member's Paystack authorization code, which only this module
 * can resolve without creating a module dependency cycle.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contributions/auto-debit")
@Tag(name = "Auto-Debit", description = "Recurring contributions charged automatically via a saved Paystack card")
public class AutoDebitSetupController {

    private final AutoDebitSetupService autoDebitSetupService;

    @PostMapping
    @Operation(
            summary = "Set up or replace an auto-debit mandate",
            description = "Requires the member to have already made at least one Paystack contribution, so a "
                    + "reusable card authorization exists to charge going forward. Replaces (cancels) any "
                    + "existing mandate for the member.")
    public ResponseEntity<ApiResponse<AutoDebitMandateSummary>> setUpOrReplace(
            @Valid @RequestBody AutoDebitSetupRequest request,
            Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        AutoDebitMandateSummary response =
                autoDebitSetupService.setUpOrReplace(memberId, request.amount(), request.periodicity());
        return ResponseEntity.ok(ApiResponse.success("Auto-debit mandate set up successfully", response));
    }
}
