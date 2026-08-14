package com.silo.contribution.event;

import com.silo.common.event.DomainEvent;
import com.silo.contribution.entity.ContributionSource;

import java.math.BigDecimal;
import java.util.UUID;

public class ContributionMadeEvent extends DomainEvent {

    private final UUID contributionId;
    private final UUID memberId;
    private final BigDecimal amount;
    private final ContributionSource source;

    public ContributionMadeEvent(UUID contributionId, UUID memberId,
                                 BigDecimal amount,
                                 ContributionSource source) {
        super();
        this.contributionId = contributionId;
        this.memberId = memberId;
        this.amount = amount;
        this.source = source;
    }

    public UUID getContributionId() {
        return contributionId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ContributionSource getSource() {
        return source;
    }
}
