package com.silo.loan;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Applies a repayment's effect on loan/installment state synchronously, in
 * the same transaction as the repayment being recorded - unlike the ledger
 * posting (an AFTER_COMMIT side effect), overpayment must be rejected
 * before the repayment is ever persisted.
 */
public interface LoanProgressionRecorder {

    /**
     * @throws com.silo.common.exception.BusinessRuleViolationException if amount exceeds the loan's outstanding balance
     */
    void applyRepayment(UUID loanId, BigDecimal amount);

    /**
     * @throws com.silo.common.exception.BusinessRuleViolationException if amount exceeds the liability's remaining balance
     */
    void applyLiabilityRepayment(UUID liabilityId, BigDecimal amount);
}
