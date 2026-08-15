package com.silo.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopContributorResponse(UUID memberId, BigDecimal totalContributions) {
}
