package com.silo.accounting.controller;

import com.silo.accounting.dto.LedgerAccountBalanceResponse;
import com.silo.accounting.dto.TrialBalanceResponse;
import com.silo.accounting.service.LedgerQueryService;
import com.silo.common.response.ApiResponse;
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
@RequestMapping("/api/ledger")
@Tag(name = "Ledger", description = "Account balances and trial balance")
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;

    @GetMapping("/accounts/{id}/balance")
    @Operation(summary = "Current balance for a ledger account")
    public ResponseEntity<ApiResponse<LedgerAccountBalanceResponse>> getAccountBalance(
            @Parameter(description = "Ledger account id") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(ledgerQueryService.getAccountBalance(id)));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Full trial balance across the chart of accounts")
    public ResponseEntity<ApiResponse<TrialBalanceResponse>> getTrialBalance() {
        return ResponseEntity.ok(ApiResponse.success(ledgerQueryService.getTrialBalance()));
    }
}
