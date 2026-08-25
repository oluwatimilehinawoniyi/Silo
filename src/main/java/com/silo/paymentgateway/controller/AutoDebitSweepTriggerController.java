package com.silo.paymentgateway.controller;

import com.silo.common.response.ApiResponse;
import com.silo.paymentgateway.service.AutoDebitChargeSweepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY: runs the auto-debit sweep on demand instead of waiting for its
 * daily cron. Exists so the recurring-charge flow can be demoed live without
 * waiting until 2 AM. Remove once the demo is over - this bypasses the
 * schedule entirely and has no place in normal operation.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/auto-debit-sweep")
@Tag(name = "Dev Tools", description = "Temporary, demo-only endpoints - not part of the real API surface")
public class AutoDebitSweepTriggerController {

    private final AutoDebitChargeSweepService autoDebitChargeSweepService;

    @PostMapping
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "[TEMPORARY] Manually run the auto-debit charge sweep now",
            description = "Officer only. Charges every ACTIVE mandate due today or earlier, exactly like the "
                    + "nightly cron job would. For demo use only.")
    public ResponseEntity<ApiResponse<Void>> triggerSweep() {
        autoDebitChargeSweepService.sweep();
        return ResponseEntity.ok(ApiResponse.success("Auto-debit sweep run complete", null));
    }
}
