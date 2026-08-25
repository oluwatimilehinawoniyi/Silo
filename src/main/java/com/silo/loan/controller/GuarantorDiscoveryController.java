package com.silo.loan.controller;

import com.silo.common.response.ApiResponse;
import com.silo.loan.dto.AvailableGuarantorResponse;
import com.silo.loan.dto.GuarantorInviteResponse;
import com.silo.loan.service.GuarantorDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Guarantor Discovery", description = "Finding guarantors and reviewing guarantor invites")
public class GuarantorDiscoveryController {

    private final GuarantorDiscoveryService guarantorDiscoveryService;

    @GetMapping("/available-guarantors")
    @Operation(summary = "List members a loanee can choose as guarantors, with their credibility scores")
    public ResponseEntity<ApiResponse<List<AvailableGuarantorResponse>>> getAvailableGuarantors(
            Authentication authentication) {
        UUID callerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(guarantorDiscoveryService.getAvailableGuarantors(callerId)));
    }

    @GetMapping("/{memberId}/guarantor-invites")
    @Operation(summary = "List a member's pending guarantor invites, with each requester's risk tier")
    public ResponseEntity<ApiResponse<List<GuarantorInviteResponse>>> getGuarantorInvites(
            @Parameter(description = "Member id") @PathVariable UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(guarantorDiscoveryService.getPendingInvites(memberId)));
    }
}
