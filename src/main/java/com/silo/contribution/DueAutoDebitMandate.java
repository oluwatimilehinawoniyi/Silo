package com.silo.contribution;

import java.math.BigDecimal;
import java.util.UUID;

public record DueAutoDebitMandate(UUID mandateId, UUID memberId, BigDecimal amount, String paystackAuthorizationCode) {
}
