package com.silo.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanApprovalRequest(
        @NotNull(message = "Interest rate is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Interest rate cannot be negative")
        BigDecimal interestRate,

        @NotNull(message = "Duration in months is required")
        @Min(value = 1, message = "Duration must be at least 1 month")
        Integer durationMonths
) {
}
