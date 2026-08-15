package com.silo.loan;

import java.util.Optional;
import java.util.UUID;

public interface GuarantorLiabilityLookup {

    boolean exists(UUID liabilityId);

    boolean isPending(UUID liabilityId);

    boolean belongsToGuarantor(UUID liabilityId, UUID guarantorMemberId);

    Optional<UUID> findLoanId(UUID liabilityId);
}
