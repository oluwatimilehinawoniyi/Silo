package com.silo.reporting.service;

import com.silo.contribution.event.ContributionMadeEvent;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.repayment.event.RepaymentMadeEvent;
import com.silo.reporting.entity.ReportingLoanSummary;
import com.silo.reporting.entity.ReportingMemberSummary;
import com.silo.reporting.entity.ReportingProcessedEvent;
import com.silo.reporting.repository.ReportingLoanSummaryRepository;
import com.silo.reporting.repository.ReportingMemberSummaryRepository;
import com.silo.reporting.repository.ReportingProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportingProjectionListener {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DEFAULTED = "DEFAULTED";

    private final ReportingMemberSummaryRepository memberSummaryRepository;
    private final ReportingLoanSummaryRepository loanSummaryRepository;
    private final ReportingProcessedEventRepository processedEventRepository;
    private final ReportingQueryService reportingQueryService;
    private final DashboardBroadcastService dashboardBroadcastService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onContributionMade(ContributionMadeEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }

        ReportingMemberSummary summary = memberSummary(event.getMemberId());
        summary.setTotalContributions(summary.getTotalContributions().add(event.getAmount()));
        memberSummaryRepository.save(summary);

        markProcessedAndBroadcast(event.getEventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanApproved(LoanApprovedEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }

        loanSummaryRepository.save(ReportingLoanSummary.builder()
                .loanId(event.getLoanId())
                .memberId(event.getMemberId())
                .status(STATUS_ACTIVE)
                .outstandingBalance(event.getPrincipalAmount())
                .daysOverdue(0)
                .build());

        ReportingMemberSummary summary = memberSummary(event.getMemberId());
        summary.setActiveLoans(summary.getActiveLoans() + 1);
        memberSummaryRepository.save(summary);

        markProcessedAndBroadcast(event.getEventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRepaymentMade(RepaymentMadeEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }

        ReportingMemberSummary summary = memberSummary(event.getPayerMemberId());
        summary.setTotalRepayments(summary.getTotalRepayments().add(event.getAmount()));
        memberSummaryRepository.save(summary);

        if (!event.isLiabilityPayment()) {
            loanSummaryRepository.findById(event.getLoanId()).ifPresent(loanSummary -> {
                BigDecimal newBalance = loanSummary.getOutstandingBalance().subtract(event.getAmount());
                loanSummary.setOutstandingBalance(newBalance.max(BigDecimal.ZERO));
                loanSummaryRepository.save(loanSummary);
            });
        }

        markProcessedAndBroadcast(event.getEventId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanDefaulted(LoanDefaultedEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }

        loanSummaryRepository.findById(event.getLoanId()).ifPresent(loanSummary -> {
            loanSummary.setStatus(STATUS_DEFAULTED);
            loanSummaryRepository.save(loanSummary);
        });

        memberSummaryRepository.findById(event.getMemberId()).ifPresent(summary -> {
            summary.setActiveLoans(Math.max(0, summary.getActiveLoans() - 1));
            memberSummaryRepository.save(summary);
        });

        markProcessedAndBroadcast(event.getEventId());
    }

    private boolean alreadyProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    private void markProcessedAndBroadcast(UUID eventId) {
        processedEventRepository.save(new ReportingProcessedEvent(eventId, Instant.now()));
        dashboardBroadcastService.broadcast(reportingQueryService.getDashboard());
    }

    private ReportingMemberSummary memberSummary(UUID memberId) {
        return memberSummaryRepository.findById(memberId)
                .orElseGet(() -> ReportingMemberSummary.builder()
                        .memberId(memberId)
                        .totalContributions(BigDecimal.ZERO)
                        .activeLoans(0)
                        .totalRepayments(BigDecimal.ZERO)
                        .build());
    }
}
