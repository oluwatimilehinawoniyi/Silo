package com.silo.loan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddGuarantorRequest(
        @NotNull(message = "Guarantor member id is required")
        UUID guarantorMemberId
) {
}
