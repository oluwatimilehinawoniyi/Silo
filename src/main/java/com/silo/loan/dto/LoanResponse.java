package com.silo.loan.dto;

import com.silo.loan.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID loanRequestId,
        UUID memberId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer durationMonths,
        LocalDateTime disbursedDate,
        LoanStatus status,
        BigDecimal outstandingBalance
) {
}
