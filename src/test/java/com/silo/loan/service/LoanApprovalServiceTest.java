package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanApprovalRequest;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanResponse;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRepository;
import com.silo.loan.repository.LoanRequestRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApprovalServiceTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    @Mock
    private LoanInstallmentService loanInstallmentService;

    @Mock
    private BorrowerRiskService borrowerRiskService;

    @Mock
    private GuarantorCredibilityService guarantorCredibilityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LoanApprovalService loanApprovalService;

    private static final UUID LOAN_REQUEST_ID = UUID.randomUUID();
    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();
    private static final UUID OFFICER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loanApprovalService = new LoanApprovalService(
                loanRequestRepository, loanRepository, loanGuarantorRepository,
                loanInstallmentService, borrowerRiskService, guarantorCredibilityService, eventPublisher);
    }

    private LoanRequest pendingRequest() {
        return LoanRequest.builder()
                .id(LOAN_REQUEST_ID)
                .memberId(BORROWER_ID)
                .amountRequested(new BigDecimal("50000.00"))
                .purpose("Equipment")
                .status(LoanRequestStatus.PENDING)
                .build();
    }

    private LoanGuarantor acceptedGuarantor() {
        return LoanGuarantor.builder()
                .id(UUID.randomUUID()).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.ACCEPTED).build();
    }

    @Test
    @DisplayName("approve rejects a loan request that doesn't exist")
    void approve_throwsResourceNotFound_whenRequestMissing() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("approve rejects a request that has already been decided")
    void approve_throwsBusinessRuleViolation_whenNotPending() {
        LoanRequest decided = pendingRequest();
        decided.setStatus(LoanRequestStatus.APPROVED);
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("approve rejects a request with no ACCEPTED guarantor")
    void approve_throwsBusinessRuleViolation_whenNoAcceptedGuarantor() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of());

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve rejects a borrower with an active default")
    void approve_throwsBusinessRuleViolation_whenBorrowerHasActiveDefault() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(acceptedGuarantor()));
        when(borrowerRiskService.hasActiveDefault(BORROWER_ID)).thenReturn(true);

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve rejects when an accepted guarantor is below the minimum credibility")
    void approve_throwsBusinessRuleViolation_whenGuarantorNotCredible() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(acceptedGuarantor()));
        when(borrowerRiskService.hasActiveDefault(BORROWER_ID)).thenReturn(false);
        when(guarantorCredibilityService.meetsMinimumCredibility(GUARANTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve rejects a borrower who already has an active loan")
    void approve_throwsBusinessRuleViolation_whenBorrowerHasActiveLoan() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(acceptedGuarantor()));
        when(borrowerRiskService.hasActiveDefault(BORROWER_ID)).thenReturn(false);
        when(loanRepository.existsByMemberIdAndStatus(BORROWER_ID, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve rejects an officer approving their own loan request")
    void approve_throwsBusinessRuleViolation_whenOfficerApprovesOwnRequest() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));

        assertThatThrownBy(() -> loanApprovalService.approve(
                LOAN_REQUEST_ID, new LoanApprovalRequest(new BigDecimal("12.5"), 12), BORROWER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve creates the Loan, generates the schedule, marks the request APPROVED, and publishes LoanApprovedEvent")
    void approve_createsLoan_whenAllGatesPass() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanGuarantorRepository.findByLoanRequestIdAndStatus(LOAN_REQUEST_ID, GuarantorStatus.ACCEPTED))
                .thenReturn(List.of(acceptedGuarantor()));
        when(borrowerRiskService.hasActiveDefault(BORROWER_ID)).thenReturn(false);
        when(guarantorCredibilityService.meetsMinimumCredibility(GUARANTOR_ID)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(loanRequestRepository.save(any(LoanRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanApprovalRequest request = new LoanApprovalRequest(new BigDecimal("12.5"), 12);
        LoanResponse response = loanApprovalService.approve(LOAN_REQUEST_ID, request, OFFICER_ID);

        assertThat(response.memberId()).isEqualTo(BORROWER_ID);
        assertThat(response.loanRequestId()).isEqualTo(LOAN_REQUEST_ID);
        assertThat(response.principalAmount()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(response.interestRate()).isEqualTo(new BigDecimal("12.5"));
        assertThat(response.durationMonths()).isEqualTo(12);
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);

        verify(loanInstallmentService).generateSchedule(any(Loan.class));

        ArgumentCaptor<LoanRequest> requestCaptor = ArgumentCaptor.forClass(LoanRequest.class);
        verify(loanRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(LoanRequestStatus.APPROVED);

        ArgumentCaptor<LoanApprovedEvent> eventCaptor = ArgumentCaptor.forClass(LoanApprovedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getMemberId()).isEqualTo(BORROWER_ID);
        assertThat(eventCaptor.getValue().getLoanRequestId()).isEqualTo(LOAN_REQUEST_ID);
    }

    @Test
    @DisplayName("reject rejects a loan request that doesn't exist")
    void reject_throwsResourceNotFound_whenRequestMissing() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApprovalService.reject(LOAN_REQUEST_ID, OFFICER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("reject rejects a request that has already been decided")
    void reject_throwsBusinessRuleViolation_whenNotPending() {
        LoanRequest decided = pendingRequest();
        decided.setStatus(LoanRequestStatus.REJECTED);
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> loanApprovalService.reject(LOAN_REQUEST_ID, OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("reject rejects an officer rejecting their own loan request")
    void reject_throwsBusinessRuleViolation_whenOfficerRejectsOwnRequest() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));

        assertThatThrownBy(() -> loanApprovalService.reject(LOAN_REQUEST_ID, BORROWER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("reject marks a PENDING request REJECTED without touching Loan")
    void reject_marksRequestRejected_whenPending() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(loanRequestRepository.save(any(LoanRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanRequestResponse response = loanApprovalService.reject(LOAN_REQUEST_ID, OFFICER_ID);

        assertThat(response.status()).isEqualTo(LoanRequestStatus.REJECTED);
        verify(loanRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
