package com.silo.reporting.service;

import com.silo.reporting.dto.DashboardResponse;
import com.silo.reporting.dto.MemberReportSummaryResponse;
import com.silo.reporting.dto.TopContributorResponse;
import com.silo.reporting.entity.ReportingMemberSummary;
import com.silo.reporting.repository.ReportingLoanSummaryRepository;
import com.silo.reporting.repository.ReportingMemberSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingQueryService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DEFAULTED = "DEFAULTED";

    private final ReportingMemberSummaryRepository memberSummaryRepository;
    private final ReportingLoanSummaryRepository loanSummaryRepository;

    public DashboardResponse getDashboard() {
        long activeLoans = loanSummaryRepository.countByStatus(STATUS_ACTIVE);
        long totalLoans = loanSummaryRepository.count();
        long defaultedLoans = loanSummaryRepository.countByStatus(STATUS_DEFAULTED);

        BigDecimal totalContributions = memberSummaryRepository.sumTotalContributions();
        BigDecimal outstandingBalance = loanSummaryRepository.sumOutstandingBalanceByStatus(STATUS_ACTIVE);
        BigDecimal defaultRate = totalLoans == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(defaultedLoans).divide(BigDecimal.valueOf(totalLoans), 4, RoundingMode.HALF_UP);

        return new DashboardResponse(activeLoans, totalContributions, outstandingBalance, defaultRate);
    }

    public MemberReportSummaryResponse getMemberSummary(UUID memberId) {
        return memberSummaryRepository.findById(memberId)
                .map(this::toMemberSummaryResponse)
                .orElse(new MemberReportSummaryResponse(memberId, BigDecimal.ZERO, 0, BigDecimal.ZERO));
    }

    public List<TopContributorResponse> getTopContributors(int limit) {
        return memberSummaryRepository.findAllByOrderByTotalContributionsDesc(PageRequest.of(0, limit))
                .stream()
                .map(summary -> new TopContributorResponse(summary.getMemberId(), summary.getTotalContributions()))
                .toList();
    }

    private MemberReportSummaryResponse toMemberSummaryResponse(ReportingMemberSummary summary) {
        return new MemberReportSummaryResponse(
                summary.getMemberId(),
                summary.getTotalContributions(),
                summary.getActiveLoans(),
                summary.getTotalRepayments());
    }
}
