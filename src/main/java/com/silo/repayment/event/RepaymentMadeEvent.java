package com.silo.repayment.event;

import com.silo.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public class RepaymentMadeEvent extends DomainEvent {

    private final UUID repaymentId;
    private final UUID loanId;
    private final UUID payerMemberId;
    private final BigDecimal amount;
    private final UUID liabilityId;

    public RepaymentMadeEvent(UUID repaymentId, UUID loanId, UUID payerMemberId, BigDecimal amount, UUID liabilityId) {
        super();
        this.repaymentId = repaymentId;
        this.loanId = loanId;
        this.payerMemberId = payerMemberId;
        this.amount = amount;
        this.liabilityId = liabilityId;
    }

    public UUID getRepaymentId() {
        return repaymentId;
    }

    public UUID getLoanId() {
        return loanId;
    }

    public UUID getPayerMemberId() {
        return payerMemberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public boolean isLiabilityPayment() {
        return liabilityId != null;
    }
}
