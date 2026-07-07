package com.silo.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        String idType,
        String idNumber,
        String idDocumentRef
) {
}
