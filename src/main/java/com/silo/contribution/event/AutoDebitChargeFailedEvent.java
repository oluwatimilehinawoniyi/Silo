package com.silo.contribution.event;

import com.silo.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published on every failed auto-debit attempt, not just the final one -
 * a member should hear about a declined card immediately, not only once
 * the mandate gives up entirely. {@code mandateStopped} distinguishes the
 * final failure (retry budget exhausted, mandate now FAILED, no further
 * attempts) from an earlier one that will still retry next period.
 */
public class AutoDebitChargeFailedEvent extends DomainEvent {

    private final UUID mandateId;
    private final UUID memberId;
    private final BigDecimal amount;
    private final String reason;
    private final boolean mandateStopped;

    public AutoDebitChargeFailedEvent(
            UUID mandateId, UUID memberId, BigDecimal amount, String reason, boolean mandateStopped) {
        this.mandateId = mandateId;
        this.memberId = memberId;
        this.amount = amount;
        this.reason = reason;
        this.mandateStopped = mandateStopped;
    }

    public UUID getMandateId() {
        return mandateId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public boolean isMandateStopped() {
        return mandateStopped;
    }
}
