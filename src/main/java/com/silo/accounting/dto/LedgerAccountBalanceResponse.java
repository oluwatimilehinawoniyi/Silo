package com.silo.accounting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerAccountBalanceResponse(UUID accountId, String code, String name, BigDecimal balance) {
}
