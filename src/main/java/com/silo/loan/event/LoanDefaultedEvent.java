package com.silo.loan.event;

import com.silo.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class LoanDefaultedEvent extends DomainEvent {

    private final UUID loanId;
    private final UUID memberId;
    private final BigDecimal outstandingBalance;
    private final List<UUID> guarantorLiabilityIds;

    public LoanDefaultedEvent(
            UUID loanId, UUID memberId, BigDecimal outstandingBalance, List<UUID> guarantorLiabilityIds) {
        super();
        this.loanId = loanId;
        this.memberId = memberId;
        this.outstandingBalance = outstandingBalance;
        this.guarantorLiabilityIds = guarantorLiabilityIds;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public List<UUID> getGuarantorLiabilityIds() {
        return guarantorLiabilityIds;
    }
}
