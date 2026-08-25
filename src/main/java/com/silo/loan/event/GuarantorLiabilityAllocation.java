package com.silo.loan.event;

import java.math.BigDecimal;
import java.util.UUID;

public class GuarantorLiabilityAllocation {

    private final UUID liabilityId;
    private final UUID guarantorMemberId;
    private final BigDecimal amount;

    public GuarantorLiabilityAllocation(UUID liabilityId, UUID guarantorMemberId, BigDecimal amount) {
        this.liabilityId = liabilityId;
        this.guarantorMemberId = guarantorMemberId;
        this.amount = amount;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public UUID getGuarantorMemberId() {
        return guarantorMemberId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
