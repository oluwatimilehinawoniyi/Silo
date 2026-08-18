package com.silo.paymentgateway.dto;

import com.silo.contribution.AutoDebitPeriodicity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AutoDebitSetupRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Periodicity is required")
        AutoDebitPeriodicity periodicity
) {
}
