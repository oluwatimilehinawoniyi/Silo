package com.silo.loan.dto;

import com.silo.loan.enums.RiskTier;

import java.util.UUID;

public record BorrowerRiskProfileResponse(
        UUID memberId,
        int totalLoans,
        int defaultedLoans,
        int lateLoanPayments,
        RiskTier currentRiskTier
) {
}
