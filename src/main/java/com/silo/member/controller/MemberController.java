package com.silo.member.controller;

import com.silo.common.response.ApiResponse;
import com.silo.member.dto.KycDocumentUploadResponse;
import com.silo.member.dto.MemberKycUpdateRequest;
import com.silo.member.dto.MemberProfileUpdateRequest;
import com.silo.member.dto.MemberRequest;
import com.silo.member.dto.MemberResponse;
import com.silo.member.dto.MemberStatusUpdateRequest;
import com.silo.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(
        name = "Members",
        description = "Member registration, profile, status and KYC management")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @Operation(
            summary = "Register a new member",
            description = "Creates a member with kycStatus defaulted to PENDING")
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @Valid @RequestBody MemberRequest request) {
        MemberResponse response = memberService.createMember(request);
        return ResponseEntity.ok(
                ApiResponse.success("Member registered successfully",
                        response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "View a member profile")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @Parameter(description = "Member id") @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(memberService.getMember(id)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a member profile",
            description = "Updates contact details and KYC document references")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
            @Parameter(description = "Member id") @PathVariable UUID id,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        MemberResponse response = memberService.updateProfile(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully",
                        response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "Change member status",
            description = "Officer only")
    public ResponseEntity<ApiResponse<MemberResponse>> updateStatus(
            @Parameter(description = "Member id") @PathVariable UUID id,
            @Valid @RequestBody MemberStatusUpdateRequest request,
            Authentication authentication) {
        UUID officerId = UUID.fromString(authentication.getName());
        MemberResponse response = memberService.updateStatus(id, request, officerId);
        return ResponseEntity.ok(
                ApiResponse.success("Member status updated successfully",
                        response));
    }

    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "Approve or reject KYC",
            description = "Officer only")
    public ResponseEntity<ApiResponse<MemberResponse>> updateKycStatus(
            @Parameter(description = "Member id") @PathVariable UUID id,
            @Valid @RequestBody MemberKycUpdateRequest request,
            Authentication authentication) {
        UUID officerId = UUID.fromString(authentication.getName());
        MemberResponse response =
                memberService.updateKycStatus(id, request, officerId);
        return ResponseEntity.ok(
                ApiResponse.success("KYC status updated successfully",
                        response));
    }

    @PostMapping("/{id}/kyc-document")
    @Operation(
            summary = "Upload a KYC document",
            description = "Accepts an image or PDF, stores it, sets it as the member's idDocumentRef, and "
                    + "attempts OCR extraction of idType/idNumber for the frontend to offer as a pre-fill. "
                    + "extracted is null if OCR found nothing usable - it never blocks the upload or changes "
                    + "kycStatus by itself.")
    public ResponseEntity<ApiResponse<KycDocumentUploadResponse>> uploadKycDocument(
            @Parameter(description = "Member id") @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        KycDocumentUploadResponse response = memberService.uploadKycDocument(id, file);
        return ResponseEntity.ok(
                ApiResponse.success("KYC document uploaded successfully",
                        response));
    }

    @GetMapping("/pending-kyc")
    @PreAuthorize("hasRole('OFFICER')")
    @Operation(
            summary = "List members awaiting KYC review",
            description = "Officer only")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> listPendingKyc() {
        return ResponseEntity.ok(ApiResponse.success(memberService.listPendingKyc()));
    }
}
