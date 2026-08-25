package com.silo.loan;

import java.util.UUID;

public interface LoanLookup {

    boolean exists(UUID loanId);

    boolean isActive(UUID loanId);

    boolean belongsToMember(UUID loanId, UUID memberId);
}
