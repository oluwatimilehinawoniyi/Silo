package com.silo.repayment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RepaymentResponse(
        UUID id,
        UUID loanId,
        UUID payerMemberId,
        UUID liabilityId,
        BigDecimal amount,
        String reference,
        LocalDateTime paymentDate
) {
}
