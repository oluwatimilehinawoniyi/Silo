package com.silo.loan.event;

import com.silo.common.event.DomainEvent;

import java.util.UUID;

public class GuarantorInvitedEvent extends DomainEvent {

    private final UUID loanGuarantorId;
    private final UUID loanRequestId;
    private final UUID guarantorMemberId;

    public GuarantorInvitedEvent(UUID loanGuarantorId, UUID loanRequestId, UUID guarantorMemberId) {
        super();
        this.loanGuarantorId = loanGuarantorId;
        this.loanRequestId = loanRequestId;
        this.guarantorMemberId = guarantorMemberId;
    }

    public UUID getLoanGuarantorId() {
        return loanGuarantorId;
    }

    public UUID getLoanRequestId() {
        return loanRequestId;
    }

    public UUID getGuarantorMemberId() {
        return guarantorMemberId;
    }
}
