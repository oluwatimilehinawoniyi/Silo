package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.entity.GuarantorLiability;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LiabilityStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanProgressionServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private GuarantorLiabilityRepository guarantorLiabilityRepository;

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    @Mock
    private GuarantorCredibilityService guarantorCredibilityService;

    private LoanProgressionService loanProgressionService;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID LOAN_REQUEST_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loanProgressionService = new LoanProgressionService(
                loanRepository, loanInstallmentRepository, guarantorLiabilityRepository,
                loanGuarantorRepository, guarantorCredibilityService);
    }

    private Loan activeLoan(BigDecimal outstandingBalance) {
        return Loan.builder()
                .id(LOAN_ID)
                .loanRequestId(LOAN_REQUEST_ID)
                .memberId(UUID.randomUUID())
                .principalAmount(new BigDecimal("6000.00"))
                .interestRate(new BigDecimal("0.10"))
                .durationMonths(4)
                .status(LoanStatus.ACTIVE)
                .outstandingBalance(outstandingBalance)
                .build();
    }

    private LoanInstallment installment(int number, BigDecimal expected, BigDecimal paid, InstallmentStatus status) {
        return LoanInstallment.builder()
                .id(UUID.randomUUID())
                .installmentNumber(number)
                .dueDate(LocalDateTime.now())
                .expectedAmount(expected)
                .amountPaid(paid)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("applyRepayment rejects an amount greater than the outstanding balance")
    void applyRepayment_throwsBusinessRuleViolation_whenAmountExceedsBalance() {
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(activeLoan(new BigDecimal("1000.00"))));

        assertThatThrownBy(() -> loanProgressionService.applyRepayment(LOAN_ID, new BigDecimal("1500.00")))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyRepayment rejects a loan that doesn't exist")
    void applyRepayment_throwsResourceNotFound_whenLoanMissing() {
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanProgressionService.applyRepayment(LOAN_ID, new BigDecimal("100.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("applyRepayment decrements the balance and marks a fully-covered installment PAID, leaving the loan ACTIVE")
    void applyRepayment_partialPayment_decrementsBalanceAndAdvancesInstallment() {
        Loan loan = activeLoan(new BigDecimal("6000.00"));
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        LoanInstallment first = installment(1, new BigDecimal("1500.00"), BigDecimal.ZERO, InstallmentStatus.PENDING);
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(first));

        loanProgressionService.applyRepayment(LOAN_ID, new BigDecimal("1500.00"));

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("4500.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(first.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(first.getAmountPaid()).isEqualByComparingTo("1500.00");
        verify(guarantorCredibilityService, never()).recordSuccessfulGuarantee(any());
    }

    @Test
    @DisplayName("applyRepayment closes the loan and rewards accepted guarantors when the balance hits zero")
    void applyRepayment_finalPayment_closesLoanAndRewardsGuarantors() {
        Loan loan = activeLoan(new BigDecimal("1500.00"));
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        LoanInstallment last = installment(4, new BigDecimal("1500.00"), BigDecimal.ZERO, InstallmentStatus.PENDING);
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(last));
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(UUID.randomUUID()).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.ACCEPTED).build();
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(guarantor));

        loanProgressionService.applyRepayment(LOAN_ID, new BigDecimal("1500.00"));

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("0.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        verify(guarantorCredibilityService).recordSuccessfulGuarantee(GUARANTOR_ID);
    }

    @Test
    @DisplayName("applyRepayment closes a recovered DEFAULTED loan without rewarding guarantors")
    void applyRepayment_finalPayment_onDefaultedLoan_closesWithoutReward() {
        Loan loan = activeLoan(new BigDecimal("1500.00"));
        loan.setStatus(LoanStatus.DEFAULTED);
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of());

        loanProgressionService.applyRepayment(LOAN_ID, new BigDecimal("1500.00"));

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("0.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        verify(guarantorCredibilityService, never()).recordSuccessfulGuarantee(any());
        verify(loanGuarantorRepository, never()).findByLoanRequestIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("applyLiabilityRepayment rejects an amount greater than the liability's remaining balance")
    void applyLiabilityRepayment_throwsBusinessRuleViolation_whenAmountExceedsRemaining() {
        UUID liabilityId = UUID.randomUUID();
        GuarantorLiability liability = GuarantorLiability.builder()
                .id(liabilityId).loanId(LOAN_ID).guarantorMemberId(GUARANTOR_ID)
                .amount(new BigDecimal("2000.00")).amountPaid(new BigDecimal("1500.00"))
                .status(LiabilityStatus.PENDING).build();
        when(guarantorLiabilityRepository.findById(liabilityId)).thenReturn(Optional.of(liability));

        assertThatThrownBy(() -> loanProgressionService.applyLiabilityRepayment(liabilityId, new BigDecimal("600.00")))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyLiabilityRepayment marks the liability PAID and reduces the loan's balance when fully covered")
    void applyLiabilityRepayment_fullPayment_marksLiabilityPaidAndReducesLoanBalance() {
        UUID liabilityId = UUID.randomUUID();
        GuarantorLiability liability = GuarantorLiability.builder()
                .id(liabilityId).loanId(LOAN_ID).guarantorMemberId(GUARANTOR_ID)
                .amount(new BigDecimal("2000.00")).amountPaid(BigDecimal.ZERO)
                .status(LiabilityStatus.PENDING).build();
        when(guarantorLiabilityRepository.findById(liabilityId)).thenReturn(Optional.of(liability));
        Loan loan = activeLoan(new BigDecimal("2000.00"));
        loan.setStatus(LoanStatus.DEFAULTED);
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of());

        loanProgressionService.applyLiabilityRepayment(liabilityId, new BigDecimal("2000.00"));

        assertThat(liability.getStatus()).isEqualTo(LiabilityStatus.PAID);
        assertThat(liability.getAmountPaid()).isEqualByComparingTo("2000.00");
        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("0.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        verify(guarantorCredibilityService, never()).recordSuccessfulGuarantee(any());
    }
}
