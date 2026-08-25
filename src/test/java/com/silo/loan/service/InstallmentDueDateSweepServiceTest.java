package com.silo.loan.service;

import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.repository.LoanInstallmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallmentDueDateSweepServiceTest {

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private BorrowerRiskService borrowerRiskService;

    private InstallmentDueDateSweepService sweepService;

    private static final int GRACE_PERIOD_DAYS = 3;

    private void setUp() {
        sweepService = new InstallmentDueDateSweepService(
                loanInstallmentRepository, borrowerRiskService, GRACE_PERIOD_DAYS);
    }

    @Test
    @DisplayName("marks overdue PENDING installments LATE and recomputes each affected borrower's risk profile")
    void sweepOverdueInstallments_marksLateAndRecomputesRiskProfiles() {
        setUp();
        UUID memberId = UUID.randomUUID();
        Loan loan = Loan.builder().id(UUID.randomUUID()).memberId(memberId).build();
        LoanInstallment overdue = LoanInstallment.builder()
                .id(UUID.randomUUID())
                .loan(loan)
                .installmentNumber(1)
                .dueDate(LocalDateTime.now().minusDays(10))
                .status(InstallmentStatus.PENDING)
                .build();
        when(loanInstallmentRepository.findByStatusAndDueDateBefore(eq(InstallmentStatus.PENDING), any()))
                .thenReturn(List.of(overdue));
        when(loanInstallmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int marked = sweepService.sweepOverdueInstallments();

        assertThat(marked).isEqualTo(1);
        assertThat(overdue.getStatus()).isEqualTo(InstallmentStatus.LATE);
        verify(borrowerRiskService).recomputeAndSave(memberId);
    }

    @Test
    @DisplayName("does nothing when there are no overdue installments")
    void sweepOverdueInstallments_doesNothing_whenNoneOverdue() {
        setUp();
        when(loanInstallmentRepository.findByStatusAndDueDateBefore(eq(InstallmentStatus.PENDING), any()))
                .thenReturn(List.of());

        int marked = sweepService.sweepOverdueInstallments();

        assertThat(marked).isZero();
        verify(borrowerRiskService, never()).recomputeAndSave(any());
    }

    @Test
    @DisplayName("recomputes each affected borrower's risk profile only once, even with multiple overdue loans")
    void sweepOverdueInstallments_recomputesOncePerBorrower() {
        setUp();
        UUID memberId = UUID.randomUUID();
        Loan loanOne = Loan.builder().id(UUID.randomUUID()).memberId(memberId).build();
        Loan loanTwo = Loan.builder().id(UUID.randomUUID()).memberId(memberId).build();
        LoanInstallment first = LoanInstallment.builder()
                .id(UUID.randomUUID()).loan(loanOne).installmentNumber(1)
                .dueDate(LocalDateTime.now().minusDays(10)).status(InstallmentStatus.PENDING).build();
        LoanInstallment second = LoanInstallment.builder()
                .id(UUID.randomUUID()).loan(loanTwo).installmentNumber(1)
                .dueDate(LocalDateTime.now().minusDays(10)).status(InstallmentStatus.PENDING).build();
        when(loanInstallmentRepository.findByStatusAndDueDateBefore(eq(InstallmentStatus.PENDING), any()))
                .thenReturn(List.of(first, second));
        when(loanInstallmentRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        sweepService.sweepOverdueInstallments();

        verify(borrowerRiskService, times(1)).recomputeAndSave(memberId);
    }
}
