package com.silo.repayment.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.GuarantorLiabilityLookup;
import com.silo.loan.LoanLookup;
import com.silo.loan.LoanProgressionRecorder;
import com.silo.repayment.dto.LiabilityRepaymentRequest;
import com.silo.repayment.dto.RepaymentResponse;
import com.silo.repayment.entity.Repayment;
import com.silo.repayment.event.RepaymentMadeEvent;
import com.silo.repayment.repository.RepaymentRepository;
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
class RepaymentServiceLiabilityTest {

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private LoanLookup loanLookup;

    @Mock
    private GuarantorLiabilityLookup guarantorLiabilityLookup;

    @Mock
    private LoanProgressionRecorder loanProgressionRecorder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RepaymentService repaymentService;

    private static final UUID LIABILITY_ID = UUID.randomUUID();
    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");
    private static final String REFERENCE = "liability-txn-001";

    @Test
    @DisplayName("recordLiabilityRepayment rejects a liability that doesn't exist")
    void recordLiabilityRepayment_throwsResourceNotFound_whenLiabilityMissing() {
        when(guarantorLiabilityLookup.exists(LIABILITY_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordLiabilityRepayment(
                GUARANTOR_ID, new LiabilityRepaymentRequest(LIABILITY_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordLiabilityRepayment rejects a liability that isn't PENDING")
    void recordLiabilityRepayment_throwsBusinessRuleViolation_whenNotPending() {
        when(guarantorLiabilityLookup.exists(LIABILITY_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.isPending(LIABILITY_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordLiabilityRepayment(
                GUARANTOR_ID, new LiabilityRepaymentRequest(LIABILITY_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordLiabilityRepayment rejects a payer who isn't the assigned guarantor")
    void recordLiabilityRepayment_throwsAccessDenied_whenPayerNotGuarantor() {
        when(guarantorLiabilityLookup.exists(LIABILITY_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.isPending(LIABILITY_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.belongsToGuarantor(LIABILITY_ID, GUARANTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordLiabilityRepayment(
                GUARANTOR_ID, new LiabilityRepaymentRequest(LIABILITY_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(AccessDeniedException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordLiabilityRepayment saves with the liability's loanId and publishes RepaymentMadeEvent")
    void recordLiabilityRepayment_savesAndPublishesEvent_whenValid() {
        when(guarantorLiabilityLookup.exists(LIABILITY_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.isPending(LIABILITY_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.belongsToGuarantor(LIABILITY_ID, GUARANTOR_ID)).thenReturn(true);
        when(guarantorLiabilityLookup.findLoanId(LIABILITY_ID)).thenReturn(Optional.of(LOAN_ID));
        when(repaymentRepository.save(any(Repayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepaymentResponse response = repaymentService.recordLiabilityRepayment(
                GUARANTOR_ID, new LiabilityRepaymentRequest(LIABILITY_ID, AMOUNT, REFERENCE));

        assertThat(response.loanId()).isEqualTo(LOAN_ID);
        assertThat(response.liabilityId()).isEqualTo(LIABILITY_ID);
        assertThat(response.payerMemberId()).isEqualTo(GUARANTOR_ID);

        ArgumentCaptor<RepaymentMadeEvent> captor = ArgumentCaptor.forClass(RepaymentMadeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isLiabilityPayment()).isTrue();
        assertThat(captor.getValue().getLiabilityId()).isEqualTo(LIABILITY_ID);
    }
}
