package com.silo.loan.service;

import com.silo.loan.entity.BorrowerRiskProfile;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.enums.RiskTier;
import com.silo.loan.repository.BorrowerRiskProfileRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowerRiskServiceTest {

    @Mock
    private BorrowerRiskProfileRepository borrowerRiskProfileRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    private BorrowerRiskService service;

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BorrowerRiskService(borrowerRiskProfileRepository, loanRepository, loanInstallmentRepository);
    }

    @Test
    @DisplayName("a member with no loans gets a LOW risk tier and zeroed counters")
    void recomputeAndSave_ratesLow_whenNoLoanHistory() {
        when(loanRepository.countByMemberId(MEMBER_ID)).thenReturn(0L);
        when(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(false);
        when(loanRepository.findIdByMemberId(MEMBER_ID)).thenReturn(List.of());
        when(borrowerRiskProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(borrowerRiskProfileRepository.save(any(BorrowerRiskProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerRiskProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getTotalLoans()).isZero();
        assertThat(profile.getCurrentRiskTier()).isEqualTo(RiskTier.LOW);
        verify(loanInstallmentRepository, never()).countByLoanIdInAndStatus(any(), any());
    }

    @Test
    @DisplayName("a member with an active default gets a HIGH risk tier")
    void recomputeAndSave_ratesHigh_whenActiveDefaultExists() {
        when(loanRepository.countByMemberId(MEMBER_ID)).thenReturn(2L);
        when(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(1L);
        when(loanRepository.existsByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(true);
        when(loanRepository.findIdByMemberId(MEMBER_ID)).thenReturn(List.of(UUID.randomUUID()));
        when(loanInstallmentRepository.countByLoanIdInAndStatus(any(), eq(InstallmentStatus.LATE))).thenReturn(0L);
        when(borrowerRiskProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(borrowerRiskProfileRepository.save(any(BorrowerRiskProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerRiskProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getCurrentRiskTier()).isEqualTo(RiskTier.HIGH);
        assertThat(profile.getDefaultedLoans()).isEqualTo(1);
    }

    @Test
    @DisplayName("3 or more late payments with no default gets a MEDIUM risk tier")
    void recomputeAndSave_ratesMedium_whenLatePaymentsAtThreshold() {
        when(loanRepository.countByMemberId(MEMBER_ID)).thenReturn(1L);
        when(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(false);
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findIdByMemberId(MEMBER_ID)).thenReturn(List.of(loanId));
        when(loanInstallmentRepository.countByLoanIdInAndStatus(List.of(loanId), InstallmentStatus.LATE))
                .thenReturn(3L);
        when(borrowerRiskProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());
        when(borrowerRiskProfileRepository.save(any(BorrowerRiskProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerRiskProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getCurrentRiskTier()).isEqualTo(RiskTier.MEDIUM);
        assertThat(profile.getLateLoanPayments()).isEqualTo(3);
    }

    @Test
    @DisplayName("recomputeAndSave updates the existing profile row rather than creating a duplicate")
    void recomputeAndSave_updatesExistingProfile() {
        BorrowerRiskProfile existing = BorrowerRiskProfile.builder()
                .memberId(MEMBER_ID).totalLoans(5).currentRiskTier(RiskTier.LOW).build();
        when(loanRepository.countByMemberId(MEMBER_ID)).thenReturn(1L);
        when(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(0L);
        when(loanRepository.existsByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(false);
        when(loanRepository.findIdByMemberId(MEMBER_ID)).thenReturn(List.of());
        when(borrowerRiskProfileRepository.findById(MEMBER_ID)).thenReturn(Optional.of(existing));
        when(borrowerRiskProfileRepository.save(any(BorrowerRiskProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerRiskProfile profile = service.recomputeAndSave(MEMBER_ID);

        assertThat(profile.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(profile.getTotalLoans()).isEqualTo(1);
        verify(borrowerRiskProfileRepository, never()).save(argThat(p -> p != existing));
    }

    @Test
    @DisplayName("hasActiveDefault delegates directly to the repository")
    void hasActiveDefault_delegatesToRepository() {
        when(loanRepository.existsByMemberIdAndStatus(MEMBER_ID, LoanStatus.DEFAULTED)).thenReturn(true);

        assertThat(service.hasActiveDefault(MEMBER_ID)).isTrue();
    }
}
