package com.silo.reporting.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long activeLoans,
        BigDecimal totalContributions,
        BigDecimal outstandingBalance,
        BigDecimal defaultRate
) {
}
