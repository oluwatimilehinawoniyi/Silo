package com.silo.contribution.dto;

import com.silo.contribution.AutoDebitPeriodicity;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record AutoDebitMandateUpdateRequest(
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        AutoDebitPeriodicity periodicity,

        AutoDebitAction action
) {
}
