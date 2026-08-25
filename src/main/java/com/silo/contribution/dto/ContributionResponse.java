package com.silo.contribution.dto;

import com.silo.contribution.entity.ContributionSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContributionResponse(
        UUID id,
        UUID memberId,
        BigDecimal amount,
        String reference,
        ContributionSource source,
        UUID recordedBy,
        LocalDateTime contributionDate
) {
}
