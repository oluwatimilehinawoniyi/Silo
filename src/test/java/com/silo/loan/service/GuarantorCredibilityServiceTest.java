package com.silo.loan.service;

import com.silo.loan.entity.GuarantorCredibilityProfile;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.repository.GuarantorCredibilityProfileRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuarantorCredibilityServiceTest {

    @Mock
    private GuarantorCredibilityProfileRepository guarantorCredibilityProfileRepository;

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    private GuarantorCredibilityService service;

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GuarantorCredibilityService(guarantorCredibilityProfileRepository, loanGuarantorRepository);
    }

    @Test
    @DisplayName("a first-time guarantor starts at the full credibility score")
    void recomputeAndSave_startsAtFullScore_whenNoHistory() {
        when(loanGuarantorRepository.countByMemberIdAndStatus(MEMBER_ID, GuarantorStatus.ACCEPTED)).thenReturn(0L);
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(guarantorCredibilityProfileRepository.save(any(GuarantorCredibilityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuarantorCredibilityProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getTimesGuaranteed()).isZero();
        assertThat(profile.getCredibilityScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("recomputeAndSave counts only ACCEPTED guarantor invites")
    void recomputeAndSave_countsAcceptedInvitesOnly() {
        when(loanGuarantorRepository.countByMemberIdAndStatus(MEMBER_ID, GuarantorStatus.ACCEPTED)).thenReturn(4L);
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(guarantorCredibilityProfileRepository.save(any(GuarantorCredibilityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuarantorCredibilityProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getTimesGuaranteed()).isEqualTo(4);
    }

    @Test
    @DisplayName("existing loansWentBad lowers the recomputed score, floored at zero")
    void recomputeAndSave_lowersScoreForBadLoans_andFloorsAtZero() {
        GuarantorCredibilityProfile existing = GuarantorCredibilityProfile.builder()
                .memberId(MEMBER_ID).loansWentBad(4).build();
        when(loanGuarantorRepository.countByMemberIdAndStatus(MEMBER_ID, GuarantorStatus.ACCEPTED)).thenReturn(4L);
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.of(existing));
        when(guarantorCredibilityProfileRepository.save(any(GuarantorCredibilityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GuarantorCredibilityProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getCredibilityScore()).isZero();
    }

    @Test
    @DisplayName("meetsMinimumCredibility defaults to true when no profile exists yet")
    void meetsMinimumCredibility_defaultsTrue_whenNoProfile() {
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThat(service.meetsMinimumCredibility(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("meetsMinimumCredibility is false below the minimum acceptable score")
    void meetsMinimumCredibility_false_whenBelowThreshold() {
        GuarantorCredibilityProfile lowScore = GuarantorCredibilityProfile.builder()
                .memberId(MEMBER_ID).credibilityScore(GuarantorCredibilityService.MINIMUM_ACCEPTABLE_SCORE - 1).build();
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.of(lowScore));

        assertThat(service.meetsMinimumCredibility(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("recordLoanWentBad increments loansWentBad and lowers the score on an existing profile")
    void recordLoanWentBad_incrementsAndLowersScore_onExistingProfile() {
        GuarantorCredibilityProfile existing = GuarantorCredibilityProfile.builder()
                .memberId(MEMBER_ID).timesGuaranteed(2).loansWentBad(0).credibilityScore(100).build();
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.of(existing));
        when(guarantorCredibilityProfileRepository.save(any(GuarantorCredibilityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordLoanWentBad(MEMBER_ID);

        ArgumentCaptor<GuarantorCredibilityProfile> captor = ArgumentCaptor.forClass(GuarantorCredibilityProfile.class);
        verify(guarantorCredibilityProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getLoansWentBad()).isEqualTo(1);
        assertThat(captor.getValue().getCredibilityScore()).isEqualTo(70);
    }

    @Test
    @DisplayName("recordLoanWentBad creates a fresh profile for a guarantor with no prior history")
    void recordLoanWentBad_createsProfile_whenNoneExists() {
        when(guarantorCredibilityProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(loanGuarantorRepository.countByMemberIdAndStatus(MEMBER_ID, GuarantorStatus.ACCEPTED)).thenReturn(1L);
        when(guarantorCredibilityProfileRepository.save(any(GuarantorCredibilityProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordLoanWentBad(MEMBER_ID);

        ArgumentCaptor<GuarantorCredibilityProfile> captor = ArgumentCaptor.forClass(GuarantorCredibilityProfile.class);
        verify(guarantorCredibilityProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getLoansWentBad()).isEqualTo(1);
        assertThat(captor.getValue().getCredibilityScore()).isEqualTo(70);
    }
}
