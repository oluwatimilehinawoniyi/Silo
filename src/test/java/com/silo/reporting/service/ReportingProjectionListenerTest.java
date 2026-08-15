package com.silo.reporting.service;

import com.silo.contribution.entity.ContributionSource;
import com.silo.contribution.event.ContributionMadeEvent;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.repayment.event.RepaymentMadeEvent;
import com.silo.reporting.dto.DashboardResponse;
import com.silo.reporting.entity.ReportingLoanSummary;
import com.silo.reporting.entity.ReportingMemberSummary;
import com.silo.reporting.repository.ReportingLoanSummaryRepository;
import com.silo.reporting.repository.ReportingMemberSummaryRepository;
import com.silo.reporting.repository.ReportingProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportingProjectionListenerTest {

    @Mock
    private ReportingMemberSummaryRepository memberSummaryRepository;

    @Mock
    private ReportingLoanSummaryRepository loanSummaryRepository;

    @Mock
    private ReportingProcessedEventRepository processedEventRepository;

    @Mock
    private ReportingQueryService reportingQueryService;

    @Mock
    private DashboardBroadcastService dashboardBroadcastService;

    private ReportingProjectionListener listener;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID LOAN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new ReportingProjectionListener(
                memberSummaryRepository, loanSummaryRepository, processedEventRepository,
                reportingQueryService, dashboardBroadcastService);
    }

    private void stubDashboard() {
        when(reportingQueryService.getDashboard()).thenReturn(
                new DashboardResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("onContributionMade adds the amount to a new member summary and broadcasts")
    void onContributionMade_addsAmount_whenNoExistingSummary() {
        ContributionMadeEvent event = new ContributionMadeEvent(
                UUID.randomUUID(), MEMBER_ID, new BigDecimal("500.00"), ContributionSource.MANUAL);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        stubDashboard();

        listener.onContributionMade(event);

        ArgumentCaptor<ReportingMemberSummary> captor = ArgumentCaptor.forClass(ReportingMemberSummary.class);
        verify(memberSummaryRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalContributions()).isEqualTo(new BigDecimal("500.00"));
        verify(dashboardBroadcastService).broadcast(any());
    }

    @Test
    @DisplayName("onContributionMade is skipped when the event was already processed (outbox redelivery)")
    void onContributionMade_skipsWhenAlreadyProcessed() {
        ContributionMadeEvent event = new ContributionMadeEvent(
                UUID.randomUUID(), MEMBER_ID, new BigDecimal("500.00"), ContributionSource.MANUAL);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        listener.onContributionMade(event);

        verify(memberSummaryRepository, never()).save(any());
        verify(dashboardBroadcastService, never()).broadcast(any());
    }

    @Test
    @DisplayName("onLoanApproved creates a loan summary and increments the member's active loan count")
    void onLoanApproved_createsLoanSummaryAndIncrementsActiveLoans() {
        LoanApprovedEvent event = new LoanApprovedEvent(
                LOAN_ID, UUID.randomUUID(), MEMBER_ID, new BigDecimal("20000.00"), LocalDateTime.now());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        stubDashboard();

        listener.onLoanApproved(event);

        ArgumentCaptor<ReportingLoanSummary> loanCaptor = ArgumentCaptor.forClass(ReportingLoanSummary.class);
        verify(loanSummaryRepository).save(loanCaptor.capture());
        assertThat(loanCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(loanCaptor.getValue().getOutstandingBalance()).isEqualTo(new BigDecimal("20000.00"));

        ArgumentCaptor<ReportingMemberSummary> memberCaptor = ArgumentCaptor.forClass(ReportingMemberSummary.class);
        verify(memberSummaryRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getActiveLoans()).isEqualTo(1);
    }

    @Test
    @DisplayName("onRepaymentMade reduces outstanding balance for a non-liability repayment")
    void onRepaymentMade_reducesOutstandingBalance_whenNotLiabilityPayment() {
        RepaymentMadeEvent event = new RepaymentMadeEvent(
                UUID.randomUUID(), LOAN_ID, MEMBER_ID, new BigDecimal("1000.00"), null);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        ReportingLoanSummary loanSummary = ReportingLoanSummary.builder()
                .loanId(LOAN_ID).memberId(MEMBER_ID).status("ACTIVE")
                .outstandingBalance(new BigDecimal("5000.00")).daysOverdue(0).build();
        when(loanSummaryRepository.findById(LOAN_ID)).thenReturn(Optional.of(loanSummary));
        stubDashboard();

        listener.onRepaymentMade(event);

        assertThat(loanSummary.getOutstandingBalance()).isEqualTo(new BigDecimal("4000.00"));
        verify(loanSummaryRepository).save(loanSummary);
    }

    @Test
    @DisplayName("onRepaymentMade does not touch the loan balance for a liability repayment")
    void onRepaymentMade_doesNotTouchLoan_whenLiabilityPayment() {
        RepaymentMadeEvent event = new RepaymentMadeEvent(
                UUID.randomUUID(), LOAN_ID, MEMBER_ID, new BigDecimal("1000.00"), UUID.randomUUID());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        stubDashboard();

        listener.onRepaymentMade(event);

        verify(loanSummaryRepository, never()).findById(any());
        verify(loanSummaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("onLoanDefaulted marks the loan DEFAULTED and decrements the borrower's active loan count")
    void onLoanDefaulted_marksDefaultedAndDecrementsActiveLoans() {
        LoanDefaultedEvent event = new LoanDefaultedEvent(
                LOAN_ID, MEMBER_ID, new BigDecimal("15000.00"), List.of());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        ReportingLoanSummary loanSummary = ReportingLoanSummary.builder()
                .loanId(LOAN_ID).memberId(MEMBER_ID).status("ACTIVE")
                .outstandingBalance(new BigDecimal("15000.00")).daysOverdue(0).build();
        when(loanSummaryRepository.findById(LOAN_ID)).thenReturn(Optional.of(loanSummary));
        ReportingMemberSummary memberSummary = ReportingMemberSummary.builder()
                .memberId(MEMBER_ID).totalContributions(BigDecimal.ZERO).activeLoans(1)
                .totalRepayments(BigDecimal.ZERO).build();
        when(memberSummaryRepository.findById(MEMBER_ID)).thenReturn(Optional.of(memberSummary));
        stubDashboard();

        listener.onLoanDefaulted(event);

        assertThat(loanSummary.getStatus()).isEqualTo("DEFAULTED");
        assertThat(memberSummary.getActiveLoans()).isZero();
    }
}
