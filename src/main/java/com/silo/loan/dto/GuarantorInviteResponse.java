package com.silo.loan.dto;

import com.silo.loan.enums.RiskTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GuarantorInviteResponse(
        UUID id,
        UUID loanRequestId,
        UUID borrowerMemberId,
        BigDecimal amountRequested,
        String purpose,
        RiskTier borrowerRiskTier,
        LocalDateTime invitedAt
) {
}
