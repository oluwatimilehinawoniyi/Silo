package com.silo.repayment.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.LoanLookup;
import com.silo.repayment.dto.RepaymentRequest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private LoanLookup loanLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RepaymentService repaymentService;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID BORROWER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");
    private static final String REFERENCE = "txn-ref-001";

    @Test
    @DisplayName("recordBorrowerRepayment rejects a loan that doesn't exist")
    void recordBorrowerRepayment_throwsResourceNotFound_whenLoanMissing() {
        when(loanLookup.exists(LOAN_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordBorrowerRepayment(
                BORROWER_ID, new RepaymentRequest(LOAN_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordBorrowerRepayment rejects a loan that isn't ACTIVE")
    void recordBorrowerRepayment_throwsBusinessRuleViolation_whenLoanNotActive() {
        when(loanLookup.exists(LOAN_ID)).thenReturn(true);
        when(loanLookup.isActive(LOAN_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordBorrowerRepayment(
                BORROWER_ID, new RepaymentRequest(LOAN_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordBorrowerRepayment rejects a payer who isn't the loan's own borrower")
    void recordBorrowerRepayment_throwsAccessDenied_whenPayerNotBorrower() {
        when(loanLookup.exists(LOAN_ID)).thenReturn(true);
        when(loanLookup.isActive(LOAN_ID)).thenReturn(true);
        when(loanLookup.belongsToMember(LOAN_ID, BORROWER_ID)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.recordBorrowerRepayment(
                BORROWER_ID, new RepaymentRequest(LOAN_ID, AMOUNT, REFERENCE)))
                .isInstanceOf(AccessDeniedException.class);

        verify(repaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordBorrowerRepayment saves with a null liabilityId and publishes RepaymentMadeEvent")
    void recordBorrowerRepayment_savesAndPublishesEvent_whenValid() {
        when(loanLookup.exists(LOAN_ID)).thenReturn(true);
        when(loanLookup.isActive(LOAN_ID)).thenReturn(true);
        when(loanLookup.belongsToMember(LOAN_ID, BORROWER_ID)).thenReturn(true);
        when(repaymentRepository.save(any(Repayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepaymentResponse response = repaymentService.recordBorrowerRepayment(
                BORROWER_ID, new RepaymentRequest(LOAN_ID, AMOUNT, REFERENCE));

        assertThat(response.loanId()).isEqualTo(LOAN_ID);
        assertThat(response.payerMemberId()).isEqualTo(BORROWER_ID);
        assertThat(response.liabilityId()).isNull();
        assertThat(response.amount()).isEqualTo(AMOUNT);

        ArgumentCaptor<RepaymentMadeEvent> captor = ArgumentCaptor.forClass(RepaymentMadeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getLoanId()).isEqualTo(LOAN_ID);
        assertThat(captor.getValue().isLiabilityPayment()).isFalse();
    }

    @Test
    @DisplayName("getHistory returns repayments in repository order")
    void getHistory_returnsMappedRepayments() {
        Repayment repayment = Repayment.builder()
                .id(UUID.randomUUID())
                .loanId(LOAN_ID)
                .payerMemberId(BORROWER_ID)
                .amount(AMOUNT)
                .reference(REFERENCE)
                .build();
        when(repaymentRepository.findByLoanIdOrderByPaymentDateDesc(LOAN_ID)).thenReturn(List.of(repayment));

        List<RepaymentResponse> history = repaymentService.getHistory(LOAN_ID);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).loanId()).isEqualTo(LOAN_ID);
    }
}
