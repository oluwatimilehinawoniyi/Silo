package com.silo.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberProfileUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\d{11}$", message = "Phone number must be 11 digits")
        String phoneNumber,

        String idType,
        String idNumber,
        String idDocumentRef
) {
}
