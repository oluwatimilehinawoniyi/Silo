package com.silo.loan.dto;

import com.silo.loan.enums.GuarantorStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoanGuarantorResponse(
        UUID id,
        UUID loanRequestId,
        UUID memberId,
        GuarantorStatus status,
        LocalDateTime invitedAt
) {
}
