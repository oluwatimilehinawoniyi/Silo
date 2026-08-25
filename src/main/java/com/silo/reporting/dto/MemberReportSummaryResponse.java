package com.silo.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberReportSummaryResponse(
        UUID memberId,
        BigDecimal totalContributions,
        int activeLoans,
        BigDecimal totalRepayments,
        BigDecimal outstandingBalance
) {
}
