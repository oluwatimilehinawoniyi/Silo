package com.silo.loan.dto;

import com.silo.loan.enums.LoanRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanRequestResponse(
        UUID id,
        UUID memberId,
        BigDecimal amountRequested,
        String purpose,
        LoanRequestStatus status,
        LocalDateTime submittedAt
) {
}
