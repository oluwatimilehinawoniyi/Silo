package com.silo.loan.service;

import com.silo.loan.dto.BorrowerRiskProfileResponse;
import com.silo.loan.entity.BorrowerRiskProfile;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.enums.RiskTier;
import com.silo.loan.repository.BorrowerRiskProfileRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Maintains {@link BorrowerRiskProfile}, the projection the loan approval
 * workflow (T23) gates on. Recomputed on demand from Loan/LoanInstallment
 * rather than incrementally via event listeners, since the events that
 * would drive incremental updates (LoanApprovedEvent, a late-payment
 * event) don't exist yet - T23 and T24 are the natural places to trigger
 * a recompute once they do.
 */
@Service
@RequiredArgsConstructor
public class BorrowerRiskService {

    private static final int LATE_PAYMENTS_MEDIUM_RISK_THRESHOLD = 3;

    private final BorrowerRiskProfileRepository borrowerRiskProfileRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;

    @Transactional
    public BorrowerRiskProfile recomputeAndSave(UUID memberId) {
        int totalLoans = (int) loanRepository.countByMemberId(memberId);
        int defaultedLoans = (int) loanRepository.countByMemberIdAndStatus(memberId, LoanStatus.DEFAULTED);
        boolean hasActiveDefault = loanRepository.existsByMemberIdAndStatus(memberId, LoanStatus.DEFAULTED);

        List<UUID> loanIds = loanRepository.findIdByMemberId(memberId);
        int lateLoanPayments = loanIds.isEmpty() ? 0
                : (int) loanInstallmentRepository.countByLoanIdInAndStatus(loanIds, InstallmentStatus.LATE);

        RiskTier tier = computeRiskTier(hasActiveDefault, lateLoanPayments);

        BorrowerRiskProfile profile = borrowerRiskProfileRepository.findById(memberId)
                .orElseGet(() -> BorrowerRiskProfile.builder().memberId(memberId).build());
        profile.setTotalLoans(totalLoans);
        profile.setDefaultedLoans(defaultedLoans);
        profile.setLateLoanPayments(lateLoanPayments);
        profile.setCurrentRiskTier(tier);

        return borrowerRiskProfileRepository.save(profile);
    }

    public boolean hasActiveDefault(UUID memberId) {
        return loanRepository.existsByMemberIdAndStatus(memberId, LoanStatus.DEFAULTED);
    }

    public RiskTier getCurrentTier(UUID memberId) {
        return borrowerRiskProfileRepository.findById(memberId)
                .map(BorrowerRiskProfile::getCurrentRiskTier)
                .orElse(RiskTier.LOW);
    }

    public BorrowerRiskProfileResponse getProfile(UUID memberId) {
        return borrowerRiskProfileRepository.findById(memberId)
                .map(profile -> new BorrowerRiskProfileResponse(
                        profile.getMemberId(), profile.getTotalLoans(), profile.getDefaultedLoans(),
                        profile.getLateLoanPayments(), profile.getCurrentRiskTier()))
                .orElse(new BorrowerRiskProfileResponse(memberId, 0, 0, 0, RiskTier.LOW));
    }

    // LoanStatus has no "resolved default" state, so a DEFAULTED loan is always an active
    // default - hasActiveDefault and defaultedLoans > 0 are equivalent today.
    private RiskTier computeRiskTier(boolean hasActiveDefault, int lateLoanPayments) {
        if (hasActiveDefault) {
            return RiskTier.HIGH;
        }
        if (lateLoanPayments >= LATE_PAYMENTS_MEDIUM_RISK_THRESHOLD) {
            return RiskTier.MEDIUM;
        }
        return RiskTier.LOW;
    }
}
