package com.silo.contribution.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ContributionSummaryResponse(UUID memberId,
                                          BigDecimal totalAmount,
                                          long contributionCount) {
}
