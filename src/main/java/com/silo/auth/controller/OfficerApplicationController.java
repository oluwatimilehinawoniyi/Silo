package com.silo.auth.controller;

import com.silo.auth.dto.OfficerApplicationResponse;
import com.silo.auth.service.OfficerApplicationService;
import com.silo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/officer-applications")
@Tag(name = "Officer Applications", description = "Members applying to become officers, requiring two distinct officer approvals")
public class OfficerApplicationController {

    private final OfficerApplicationService officerApplicationService;

    @PostMapping
    @Operation(summary = "Apply to become an officer", description = "Submits on behalf of the authenticated member.")
    public ResponseEntity<ApiResponse<OfficerApplicationResponse>> apply(Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        OfficerApplicationResponse response = officerApplicationService.apply(memberId);
        return ResponseEntity.ok(ApiResponse.success("Officer application submitted successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "View your own officer application history")
    public ResponseEntity<ApiResponse<List<OfficerApplicationResponse>>> listMine(Authentication authentication) {
        UUID memberId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(officerApplicationService.listMine(memberId)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(summary = "View all pending officer applications", description = "Officer only.")
    public ResponseEntity<ApiResponse<List<OfficerApplicationResponse>>> listPending(Authentication authentication) {
        UUID officerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(officerApplicationService.listPending(officerId)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(summary = "Approve an officer application",
            description = "Officer only. Requires two distinct officers to approve before the applicant is elevated.")
    public ResponseEntity<ApiResponse<OfficerApplicationResponse>> approve(
            @Parameter(description = "Officer application id") @PathVariable UUID id,
            Authentication authentication) {
        UUID officerId = UUID.fromString(authentication.getName());
        OfficerApplicationResponse response = officerApplicationService.approve(id, officerId);
        return ResponseEntity.ok(ApiResponse.success("Officer application approval recorded", response));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(summary = "Reject an officer application", description = "Officer only.")
    public ResponseEntity<ApiResponse<OfficerApplicationResponse>> reject(
            @Parameter(description = "Officer application id") @PathVariable UUID id,
            Authentication authentication) {
        UUID officerId = UUID.fromString(authentication.getName());
        OfficerApplicationResponse response = officerApplicationService.reject(id, officerId);
        return ResponseEntity.ok(ApiResponse.success("Officer application rejected", response));
    }
}
