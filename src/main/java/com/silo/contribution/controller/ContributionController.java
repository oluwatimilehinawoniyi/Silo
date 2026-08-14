package com.silo.contribution.controller;

import com.silo.common.response.ApiResponse;
import com.silo.contribution.dto.ContributionRequest;
import com.silo.contribution.dto.ContributionResponse;
import com.silo.contribution.dto.ContributionSummaryResponse;
import com.silo.contribution.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/contributions")
@Tag(name = "Contributions",
        description = "Recording and viewing cooperative fund contributions")
public class ContributionController {

    private final ContributionService contributionService;

    @PostMapping
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "Record a manual contribution",
            description = "Officer only. Member must be ACTIVE and KYC_VERIFIED.")
    public ResponseEntity<ApiResponse<ContributionResponse>> recordContribution(
            @Valid @RequestBody ContributionRequest request,
            Authentication authentication) {
        UUID recordedBy = UUID.fromString(authentication.getName());
        ContributionResponse response =
                contributionService.recordManualContribution(request,
                        recordedBy);
        return ResponseEntity.ok(
                ApiResponse.success("Contribution recorded successfully",
                        response));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "View a member's contribution history")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getHistory(
            @Parameter(description = "Member id") @PathVariable
            UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(
                contributionService.getHistory(memberId)));
    }

    @GetMapping("/member/{memberId}/summary")
    @Operation(summary = "View a member's running contribution total")
    public ResponseEntity<ApiResponse<ContributionSummaryResponse>> getSummary(
            @Parameter(description = "Member id") @PathVariable
            UUID memberId) {
        return ResponseEntity.ok(ApiResponse.success(
                contributionService.getSummary(memberId)));
    }
}
