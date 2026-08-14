package com.silo.loan.event;

import com.silo.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class LoanRequestedEvent extends DomainEvent {

    private final UUID loanRequestId;
    private final UUID memberId;
    private final BigDecimal amountRequested;

    public LoanRequestedEvent(UUID loanRequestId, UUID memberId, BigDecimal amountRequested) {
        super();
        this.loanRequestId = loanRequestId;
        this.memberId = memberId;
        this.amountRequested = amountRequested;
    }

    public UUID getLoanRequestId() {
        return loanRequestId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getAmountRequested() {
        return amountRequested;
    }
}
