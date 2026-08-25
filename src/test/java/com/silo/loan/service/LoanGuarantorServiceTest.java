package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.AddGuarantorRequest;
import com.silo.loan.dto.LoanGuarantorResponse;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.event.GuarantorInvitedEvent;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.MemberLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanGuarantorServiceTest {

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanGuarantorService loanGuarantorService;

    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();
    private static final UUID LOAN_REQUEST_ID = UUID.randomUUID();

    private LoanRequest pendingRequest() {
        return LoanRequest.builder()
                .id(LOAN_REQUEST_ID)
                .memberId(BORROWER_ID)
                .amountRequested(new BigDecimal("10000.00"))
                .purpose("Business")
                .status(LoanRequestStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("addGuarantor rejects a loan request that doesn't exist")
    void addGuarantor_throwsResourceNotFound_whenLoanRequestMissing() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("addGuarantor rejects a caller who isn't the request's own borrower")
    void addGuarantor_throwsAccessDenied_whenCallerIsNotBorrower() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                UUID.randomUUID(), LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("addGuarantor rejects a request that is no longer PENDING")
    void addGuarantor_throwsBusinessRuleViolation_whenRequestNotPending() {
        LoanRequest approved = pendingRequest();
        approved.setStatus(LoanRequestStatus.APPROVED);
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("addGuarantor rejects a member guaranteeing their own request")
    void addGuarantor_throwsBusinessRuleViolation_whenSelfGuarantee() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(BORROWER_ID)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("addGuarantor rejects a guarantor that isn't ACTIVE and KYC_VERIFIED")
    void addGuarantor_throwsBusinessRuleViolation_whenGuarantorNotEligible() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(memberLookup.exists(GUARANTOR_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(GUARANTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("addGuarantor rejects a member already invited to this request")
    void addGuarantor_throwsDuplicateResource_whenAlreadyInvited() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(memberLookup.exists(GUARANTOR_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(GUARANTOR_ID)).thenReturn(true);
        when(loanGuarantorRepository.existsByLoanRequestIdAndMemberId(LOAN_REQUEST_ID, GUARANTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("addGuarantor saves a PENDING invite and publishes GuarantorInvitedEvent")
    void addGuarantor_savesAndPublishesEvent_whenValid() {
        when(loanRequestRepository.findById(LOAN_REQUEST_ID)).thenReturn(Optional.of(pendingRequest()));
        when(memberLookup.exists(GUARANTOR_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(GUARANTOR_ID)).thenReturn(true);
        when(loanGuarantorRepository.existsByLoanRequestIdAndMemberId(LOAN_REQUEST_ID, GUARANTOR_ID)).thenReturn(false);
        when(loanGuarantorRepository.save(any(LoanGuarantor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGuarantorResponse response = loanGuarantorService.addGuarantor(
                BORROWER_ID, LOAN_REQUEST_ID, new AddGuarantorRequest(GUARANTOR_ID));

        assertThat(response.loanRequestId()).isEqualTo(LOAN_REQUEST_ID);
        assertThat(response.memberId()).isEqualTo(GUARANTOR_ID);
        assertThat(response.status()).isEqualTo(GuarantorStatus.PENDING);

        ArgumentCaptor<GuarantorInvitedEvent> captor = ArgumentCaptor.forClass(GuarantorInvitedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getLoanRequestId()).isEqualTo(LOAN_REQUEST_ID);
        assertThat(captor.getValue().getGuarantorMemberId()).isEqualTo(GUARANTOR_ID);
    }

    @Test
    @DisplayName("accept rejects a caller who isn't the invited guarantor")
    void accept_throwsAccessDenied_whenCallerIsNotInvitedGuarantor() {
        UUID guarantorRowId = UUID.randomUUID();
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(guarantorRowId).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.PENDING).build();
        when(loanGuarantorRepository.findById(guarantorRowId)).thenReturn(Optional.of(guarantor));

        assertThatThrownBy(() -> loanGuarantorService.accept(UUID.randomUUID(), guarantorRowId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("accept rejects an invite that has already been responded to")
    void accept_throwsBusinessRuleViolation_whenAlreadyResponded() {
        UUID guarantorRowId = UUID.randomUUID();
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(guarantorRowId).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.DECLINED).build();
        when(loanGuarantorRepository.findById(guarantorRowId)).thenReturn(Optional.of(guarantor));

        assertThatThrownBy(() -> loanGuarantorService.accept(GUARANTOR_ID, guarantorRowId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("accept sets status to ACCEPTED when valid")
    void accept_setsStatusAccepted_whenValid() {
        UUID guarantorRowId = UUID.randomUUID();
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(guarantorRowId).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.PENDING).build();
        when(loanGuarantorRepository.findById(guarantorRowId)).thenReturn(Optional.of(guarantor));
        when(loanGuarantorRepository.save(any(LoanGuarantor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGuarantorResponse response = loanGuarantorService.accept(GUARANTOR_ID, guarantorRowId);

        assertThat(response.status()).isEqualTo(GuarantorStatus.ACCEPTED);
    }

    @Test
    @DisplayName("decline sets status to DECLINED when valid")
    void decline_setsStatusDeclined_whenValid() {
        UUID guarantorRowId = UUID.randomUUID();
        LoanGuarantor guarantor = LoanGuarantor.builder()
                .id(guarantorRowId).loanRequestId(LOAN_REQUEST_ID).memberId(GUARANTOR_ID)
                .status(GuarantorStatus.PENDING).build();
        when(loanGuarantorRepository.findById(guarantorRowId)).thenReturn(Optional.of(guarantor));
        when(loanGuarantorRepository.save(any(LoanGuarantor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanGuarantorResponse response = loanGuarantorService.decline(GUARANTOR_ID, guarantorRowId);

        assertThat(response.status()).isEqualTo(GuarantorStatus.DECLINED);
    }
}
