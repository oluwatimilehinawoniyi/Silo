package com.silo.loan.service;

import com.silo.loan.entity.GuarantorLiability;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDetectionServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    @Mock
    private GuarantorLiabilityRepository guarantorLiabilityRepository;

    @Mock
    private BorrowerRiskService borrowerRiskService;

    @Mock
    private GuarantorCredibilityService guarantorCredibilityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DefaultDetectionService defaultDetectionService;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID LOAN_REQUEST_ID = UUID.randomUUID();
    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();
    private static final int THRESHOLD = 3;

    @BeforeEach
    void setUp() {
        defaultDetectionService = new DefaultDetectionService(
                loanRepository, loanInstallmentRepository, loanGuarantorRepository,
                guarantorLiabilityRepository, borrowerRiskService, guarantorCredibilityService,
                eventPublisher, THRESHOLD);
    }

    private Loan activeLoan() {
        return Loan.builder()
                .id(LOAN_ID).loanRequestId(LOAN_REQUEST_ID).memberId(BORROWER_ID)
                .status(LoanStatus.ACTIVE).outstandingBalance(new BigDecimal("9000.00")).build();
    }

    private LoanInstallment installment(int number, InstallmentStatus status) {
        return LoanInstallment.builder().id(UUID.randomUUID()).installmentNumber(number).status(status).build();
    }

    @Test
    @DisplayName("does not default a loan with fewer than the threshold of consecutive LATE installments")
    void detectDefaults_doesNotDefault_whenBelowThreshold() {
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(activeLoan()));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(
                installment(1, InstallmentStatus.LATE),
                installment(2, InstallmentStatus.LATE),
                installment(3, InstallmentStatus.PAID)));

        int defaulted = defaultDetectionService.detectDefaults();

        assertThat(defaulted).isZero();
        verify(loanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("defaults a loan once it reaches the threshold of consecutive LATE installments")
    void detectDefaults_defaultsLoan_whenThresholdReached() {
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(activeLoan()));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(
                installment(1, InstallmentStatus.LATE),
                installment(2, InstallmentStatus.LATE),
                installment(3, InstallmentStatus.LATE)));
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(UUID.randomUUID()).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.ACCEPTED).build();
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(guarantor));
        when(guarantorLiabilityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int defaulted = defaultDetectionService.detectDefaults();

        assertThat(defaulted).isEqualTo(1);

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        assertThat(loanCaptor.getValue().getStatus()).isEqualTo(LoanStatus.DEFAULTED);

        ArgumentCaptor<List<GuarantorLiability>> liabilityCaptor = ArgumentCaptor.forClass(List.class);
        verify(guarantorLiabilityRepository).saveAll(liabilityCaptor.capture());
        assertThat(liabilityCaptor.getValue()).hasSize(1);
        assertThat(liabilityCaptor.getValue().get(0).getAmount()).isEqualTo(new BigDecimal("9000.00"));
        assertThat(liabilityCaptor.getValue().get(0).getGuarantorMemberId()).isEqualTo(GUARANTOR_ID);

        verify(borrowerRiskService).recomputeAndSave(BORROWER_ID);
        verify(guarantorCredibilityService).recordLoanWentBad(GUARANTOR_ID);

        ArgumentCaptor<LoanDefaultedEvent> eventCaptor = ArgumentCaptor.forClass(LoanDefaultedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getLoanId()).isEqualTo(LOAN_ID);
        assertThat(eventCaptor.getValue().getMemberId()).isEqualTo(BORROWER_ID);
    }

    @Test
    @DisplayName("splits the outstanding balance equally across multiple accepted guarantors")
    void detectDefaults_splitsBalanceAcrossMultipleGuarantors() {
        Loan loan = activeLoan();
        loan.setOutstandingBalance(new BigDecimal("100.01"));
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(loan));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(
                installment(1, InstallmentStatus.LATE),
                installment(2, InstallmentStatus.LATE),
                installment(3, InstallmentStatus.LATE)));
        UUID guarantorTwoId = UUID.randomUUID();
        LoanGuarantor guarantorOne = LoanGuarantor.builder()
                .id(UUID.randomUUID()).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.ACCEPTED).build();
        LoanGuarantor guarantorTwo = LoanGuarantor.builder()
                .id(UUID.randomUUID()).loanRequestId(LOAN_REQUEST_ID).memberId(guarantorTwoId)
                .status(GuarantorStatus.ACCEPTED).build();
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(guarantorOne, guarantorTwo));
        when(guarantorLiabilityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        defaultDetectionService.detectDefaults();

        ArgumentCaptor<List<GuarantorLiability>> liabilityCaptor = ArgumentCaptor.forClass(List.class);
        verify(guarantorLiabilityRepository).saveAll(liabilityCaptor.capture());
        List<GuarantorLiability> liabilities = liabilityCaptor.getValue();
        assertThat(liabilities).hasSize(2);
        BigDecimal sum = liabilities.stream().map(GuarantorLiability::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualTo(new BigDecimal("100.01"));
    }

    @Test
    @DisplayName("does not create liabilities when there are no accepted guarantors, but still defaults the loan")
    void detectDefaults_stillDefaults_whenNoAcceptedGuarantors() {
        when(loanRepository.findByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(activeLoan()));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID)).thenReturn(List.of(
                installment(1, InstallmentStatus.LATE),
                installment(2, InstallmentStatus.LATE),
                installment(3, InstallmentStatus.LATE)));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of());
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int defaulted = defaultDetectionService.detectDefaults();

        assertThat(defaulted).isEqualTo(1);
        verify(guarantorLiabilityRepository, never()).saveAll(any());
        verify(guarantorCredibilityService, never()).recordLoanWentBad(any());
    }
}
