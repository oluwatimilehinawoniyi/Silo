package com.silo.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanRequestSubmitRequest(
        @NotNull(message = "Amount requested is required")
        @DecimalMin(value = "0.01", message = "Amount requested must be positive")
        BigDecimal amountRequested,

        @NotBlank(message = "Purpose is required")
        String purpose
) {
}
