package com.silo.loan.service;

import com.silo.loan.entity.GuarantorCredibilityProfile;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.repository.GuarantorCredibilityProfileRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Maintains {@link GuarantorCredibilityProfile}, the projection the loan
 * approval workflow (T23) gates each guarantor on. loansWentBad stays 0
 * until T25 (default detection) exists and can report which guarantors
 * ended up with an assigned liability; recomputeAndSave is meant to be
 * called again from that point once it lands.
 */
@Service
@RequiredArgsConstructor
public class GuarantorCredibilityService {

    private static final int STARTING_SCORE = 100;
    private static final int PENALTY_PER_BAD_LOAN = 30;
    static final int MINIMUM_ACCEPTABLE_SCORE = 50;

    private final GuarantorCredibilityProfileRepository guarantorCredibilityProfileRepository;
    private final LoanGuarantorRepository loanGuarantorRepository;

    @Transactional
    public GuarantorCredibilityProfile recomputeAndSave(UUID memberId) {
        int timesGuaranteed = (int) loanGuarantorRepository.countByMemberIdAndStatus(memberId, GuarantorStatus.ACCEPTED);

        GuarantorCredibilityProfile profile = guarantorCredibilityProfileRepository.findById(memberId)
                .orElseGet(() -> GuarantorCredibilityProfile.builder()
                        .memberId(memberId)
                        .loansWentBad(0)
                        .build());
        profile.setTimesGuaranteed(timesGuaranteed);
        profile.setCredibilityScore(computeScore(profile.getLoansWentBad()));

        return guarantorCredibilityProfileRepository.save(profile);
    }

    public boolean meetsMinimumCredibility(UUID memberId) {
        return guarantorCredibilityProfileRepository.findById(memberId)
                .map(profile -> profile.getCredibilityScore() >= MINIMUM_ACCEPTABLE_SCORE)
                .orElse(true);
    }

    public int getCurrentScore(UUID memberId) {
        return guarantorCredibilityProfileRepository.findById(memberId)
                .map(GuarantorCredibilityProfile::getCredibilityScore)
                .orElse(STARTING_SCORE);
    }

    private int computeScore(int loansWentBad) {
        return Math.max(0, STARTING_SCORE - loansWentBad * PENALTY_PER_BAD_LOAN);
    }
}
