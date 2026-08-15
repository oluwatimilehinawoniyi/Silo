package com.silo.reporting.service;

import com.silo.reporting.dto.DashboardResponse;
import com.silo.reporting.dto.MemberReportSummaryResponse;
import com.silo.reporting.dto.TopContributorResponse;
import com.silo.reporting.entity.ReportingMemberSummary;
import com.silo.reporting.repository.ReportingLoanSummaryRepository;
import com.silo.reporting.repository.ReportingMemberSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingQueryServiceTest {

    @Mock
    private ReportingMemberSummaryRepository memberSummaryRepository;

    @Mock
    private ReportingLoanSummaryRepository loanSummaryRepository;

    @InjectMocks
    private ReportingQueryService reportingQueryService;

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("getDashboard computes default rate as defaulted / total loans")
    void getDashboard_computesDefaultRate() {
        when(loanSummaryRepository.countByStatus("ACTIVE")).thenReturn(3L);
        when(loanSummaryRepository.count()).thenReturn(4L);
        when(loanSummaryRepository.countByStatus("DEFAULTED")).thenReturn(1L);
        when(memberSummaryRepository.sumTotalContributions()).thenReturn(new BigDecimal("100000.00"));
        when(loanSummaryRepository.sumOutstandingBalanceByStatus("ACTIVE")).thenReturn(new BigDecimal("45000.00"));

        DashboardResponse dashboard = reportingQueryService.getDashboard();

        assertThat(dashboard.activeLoans()).isEqualTo(3);
        assertThat(dashboard.totalContributions()).isEqualTo(new BigDecimal("100000.00"));
        assertThat(dashboard.outstandingBalance()).isEqualTo(new BigDecimal("45000.00"));
        assertThat(dashboard.defaultRate()).isEqualTo(new BigDecimal("0.2500"));
    }

    @Test
    @DisplayName("getDashboard defaults the rate to zero when there are no loans yet")
    void getDashboard_defaultRateZero_whenNoLoans() {
        when(loanSummaryRepository.countByStatus("ACTIVE")).thenReturn(0L);
        when(loanSummaryRepository.count()).thenReturn(0L);
        when(memberSummaryRepository.sumTotalContributions()).thenReturn(BigDecimal.ZERO);
        when(loanSummaryRepository.sumOutstandingBalanceByStatus("ACTIVE")).thenReturn(BigDecimal.ZERO);

        DashboardResponse dashboard = reportingQueryService.getDashboard();

        assertThat(dashboard.defaultRate()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getMemberSummary returns zeroed defaults for a member with no projection row yet")
    void getMemberSummary_returnsDefaults_whenNoRow() {
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        MemberReportSummaryResponse summary = reportingQueryService.getMemberSummary(MEMBER_ID);

        assertThat(summary.memberId()).isEqualTo(MEMBER_ID);
        assertThat(summary.totalContributions()).isEqualTo(BigDecimal.ZERO);
        assertThat(summary.activeLoans()).isZero();
    }

    @Test
    @DisplayName("getTopContributors maps repository results in order")
    void getTopContributors_mapsResults() {
        ReportingMemberSummary summary = ReportingMemberSummary.builder()
                .memberId(MEMBER_ID).totalContributions(new BigDecimal("9000.00"))
                .activeLoans(0).totalRepayments(BigDecimal.ZERO).build();
        when(memberSummaryRepository.findAllByOrderByTotalContributionsDesc(any())).thenReturn(List.of(summary));

        List<TopContributorResponse> result = reportingQueryService.getTopContributors(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.get(0).totalContributions()).isEqualTo(new BigDecimal("9000.00"));
    }
}
