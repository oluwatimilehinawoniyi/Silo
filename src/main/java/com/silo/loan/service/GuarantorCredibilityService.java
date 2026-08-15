package com.silo.loan.service;

import com.silo.loan.dto.GuarantorCredibilityProfileResponse;
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
 * approval workflow (T23) gates each guarantor on.
 */
@Service
@RequiredArgsConstructor
public class GuarantorCredibilityService {

    private static final int STARTING_SCORE = 100;
    private static final int PENALTY_PER_BAD_LOAN = 30;
    private static final int REWARD_PER_SUCCESSFUL_GUARANTEE = 5;
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
                        .successfulGuarantees(0)
                        .build());
        profile.setTimesGuaranteed(timesGuaranteed);
        profile.setCredibilityScore(computeScore(profile.getLoansWentBad(), profile.getSuccessfulGuarantees()));

        return guarantorCredibilityProfileRepository.save(profile);
    }

    /**
     * Rewards a guarantor whose backed loan closed without ever defaulting.
     * Capped at STARTING_SCORE - a clean guarantee history can restore a
     * damaged score over time but never exceed a fresh one.
     */
    @Transactional
    public void recordSuccessfulGuarantee(UUID guarantorMemberId) {
        GuarantorCredibilityProfile profile = guarantorCredibilityProfileRepository.findById(guarantorMemberId)
                .orElseGet(() -> GuarantorCredibilityProfile.builder()
                        .memberId(guarantorMemberId)
                        .timesGuaranteed((int) loanGuarantorRepository.countByMemberIdAndStatus(
                                guarantorMemberId, GuarantorStatus.ACCEPTED))
                        .loansWentBad(0)
                        .build());
        profile.setSuccessfulGuarantees(profile.getSuccessfulGuarantees() + 1);
        profile.setCredibilityScore(computeScore(profile.getLoansWentBad(), profile.getSuccessfulGuarantees()));

        guarantorCredibilityProfileRepository.save(profile);
    }

    public boolean meetsMinimumCredibility(UUID memberId) {
        return guarantorCredibilityProfileRepository.findById(memberId)
                .map(profile -> profile.getCredibilityScore() >= MINIMUM_ACCEPTABLE_SCORE)
                .orElse(true);
    }

    @Transactional
    public void recordLoanWentBad(UUID guarantorMemberId) {
        GuarantorCredibilityProfile profile = guarantorCredibilityProfileRepository.findById(guarantorMemberId)
                .orElseGet(() -> GuarantorCredibilityProfile.builder()
                        .memberId(guarantorMemberId)
                        .timesGuaranteed((int) loanGuarantorRepository.countByMemberIdAndStatus(
                                guarantorMemberId, GuarantorStatus.ACCEPTED))
                        .build());
        profile.setLoansWentBad(profile.getLoansWentBad() + 1);
        profile.setCredibilityScore(computeScore(profile.getLoansWentBad(), profile.getSuccessfulGuarantees()));

        guarantorCredibilityProfileRepository.save(profile);
    }

    public int getCurrentScore(UUID memberId) {
        return guarantorCredibilityProfileRepository.findById(memberId)
                .map(GuarantorCredibilityProfile::getCredibilityScore)
                .orElse(STARTING_SCORE);
    }

    public GuarantorCredibilityProfileResponse getProfile(UUID memberId) {
        return guarantorCredibilityProfileRepository.findById(memberId)
                .map(profile -> new GuarantorCredibilityProfileResponse(
                        profile.getMemberId(), profile.getTimesGuaranteed(), profile.getLoansWentBad(),
                        profile.getSuccessfulGuarantees(), profile.getCredibilityScore()))
                .orElse(new GuarantorCredibilityProfileResponse(memberId, 0, 0, 0, STARTING_SCORE));
    }

    private int computeScore(int loansWentBad, int successfulGuarantees) {
        int score = STARTING_SCORE - loansWentBad * PENALTY_PER_BAD_LOAN + successfulGuarantees * REWARD_PER_SUCCESSFUL_GUARANTEE;
        return Math.max(0, Math.min(STARTING_SCORE, score));
    }
}
