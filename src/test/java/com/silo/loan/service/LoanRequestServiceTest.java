package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanRequestSubmitRequest;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.event.LoanRequestedEvent;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanRequestServiceTest {

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LoanRequestService loanRequestService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("50000.00");
    private static final String PURPOSE = "School fees";

    @Test
    @DisplayName("submit rejects a memberId that doesn't exist")
    void submit_throwsResourceNotFound_whenMemberDoesNotExist() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> loanRequestService.submit(MEMBER_ID, new LoanRequestSubmitRequest(AMOUNT, PURPOSE)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(loanRequestRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("submit rejects a member that isn't ACTIVE and KYC_VERIFIED")
    void submit_throwsBusinessRuleViolation_whenMemberNotEligible() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> loanRequestService.submit(MEMBER_ID, new LoanRequestSubmitRequest(AMOUNT, PURPOSE)))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(loanRequestRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("submit saves a PENDING request and publishes LoanRequestedEvent")
    void submit_savesAndPublishesEvent_whenMemberEligible() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(MEMBER_ID)).thenReturn(true);
        when(loanRequestRepository.save(any(LoanRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanRequestResponse response = loanRequestService.submit(MEMBER_ID, new LoanRequestSubmitRequest(AMOUNT, PURPOSE));

        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.amountRequested()).isEqualTo(AMOUNT);
        assertThat(response.purpose()).isEqualTo(PURPOSE);
        assertThat(response.status()).isEqualTo(LoanRequestStatus.PENDING);

        ArgumentCaptor<LoanRequestedEvent> captor = ArgumentCaptor.forClass(LoanRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().getAmountRequested()).isEqualTo(AMOUNT);
    }
}
