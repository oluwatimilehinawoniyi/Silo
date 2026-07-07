package com.silo.member.dto;

import com.silo.member.enums.KYCStatus;
import jakarta.validation.constraints.NotNull;

public record MemberKycUpdateRequest(
        @NotNull(message = "KYC status is required")
        KYCStatus kycStatus
) {
}
