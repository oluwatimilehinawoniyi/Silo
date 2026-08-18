package com.silo.contribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AutoDebitMandateSummary(
        UUID id,
        BigDecimal amount,
        AutoDebitPeriodicity periodicity,
        AutoDebitMandateStatus status,
        LocalDate nextChargeDate,
        int consecutiveFailureCount,
        String lastFailureReason
) {
}
