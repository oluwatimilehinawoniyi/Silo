package com.silo.loan.dto;

import java.util.UUID;

public record GuarantorCredibilityProfileResponse(
        UUID memberId,
        int timesGuaranteed,
        int loansWentBad,
        int successfulGuarantees,
        int credibilityScore
) {
}
