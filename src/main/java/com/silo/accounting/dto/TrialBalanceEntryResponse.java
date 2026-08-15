package com.silo.accounting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TrialBalanceEntryResponse(
        UUID accountId, String code, String name, BigDecimal totalDebits, BigDecimal totalCredits) {
}
