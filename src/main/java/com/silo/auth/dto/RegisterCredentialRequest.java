package com.silo.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterCredentialRequest(
        @NotNull(message = "Member id is required")
        UUID memberId,

        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
