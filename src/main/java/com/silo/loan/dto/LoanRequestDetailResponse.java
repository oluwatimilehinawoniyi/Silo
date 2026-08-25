package com.silo.loan.dto;

import com.silo.loan.enums.LoanRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LoanRequestDetailResponse(
        UUID id,
        UUID memberId,
        BigDecimal amountRequested,
        String purpose,
        LoanRequestStatus status,
        LocalDateTime submittedAt,
        List<LoanGuarantorResponse> guarantors
) {
}
