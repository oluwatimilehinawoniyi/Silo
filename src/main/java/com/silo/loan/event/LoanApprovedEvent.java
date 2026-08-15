package com.silo.loan.event;

import com.silo.common.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanApprovedEvent extends DomainEvent {

    private final UUID loanId;
    private final UUID loanRequestId;
    private final UUID memberId;
    private final BigDecimal principalAmount;
    private final LocalDateTime disbursedDate;

    public LoanApprovedEvent(
            UUID loanId, UUID loanRequestId, UUID memberId, BigDecimal principalAmount, LocalDateTime disbursedDate) {
        super();
        this.loanId = loanId;
        this.loanRequestId = loanRequestId;
        this.memberId = memberId;
        this.principalAmount = principalAmount;
        this.disbursedDate = disbursedDate;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public UUID getLoanRequestId() {
        return loanRequestId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public LocalDateTime getDisbursedDate() {
        return disbursedDate;
    }
}
