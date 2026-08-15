package com.silo.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrialBalanceResponse(
        List<TrialBalanceEntryResponse> accounts, BigDecimal totalDebits, BigDecimal totalCredits) {
}
