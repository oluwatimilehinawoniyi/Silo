package com.silo.paymentgateway.service;

import com.silo.contribution.AutoDebitMandateLookup;
import com.silo.contribution.AutoDebitMandateRecorder;
import com.silo.contribution.ContributionRecorder;
import com.silo.contribution.DueAutoDebitMandate;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoDebitChargeSweepServiceTest {

    @Mock
    private AutoDebitMandateLookup mandateLookup;

    @Mock
    private AutoDebitMandateRecorder mandateRecorder;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private PaystackChargeClient paystackChargeClient;

    @Mock
    private ContributionRecorder contributionRecorder;

    @InjectMocks
    private AutoDebitChargeSweepService service;

    private static final UUID MANDATE_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("2000.00");

    @Test
    @DisplayName("sweep records a contribution and advances the mandate on a successful charge")
    void sweep_recordsContribution_onSuccess() {
        DueAutoDebitMandate due = new DueAutoDebitMandate(MANDATE_ID, MEMBER_ID, AMOUNT, "AUTH_1");
        when(mandateLookup.findDueMandates(any(LocalDate.class))).thenReturn(List.of(due));
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.of(new MemberSummary(MEMBER_ID, "chidi@example.com")));
        when(paystackChargeClient.chargeAuthorization("AUTH_1", AMOUNT, "chidi@example.com"))
                .thenReturn(new PaystackChargeResult(true, "ref-123", AMOUNT, null));

        service.sweep();

        verify(contributionRecorder).recordPaystackContribution(MEMBER_ID, AMOUNT, "ref-123");
        verify(mandateRecorder).recordChargeSuccess(MANDATE_ID);
        verify(mandateRecorder, never()).recordChargeFailure(any(), any());
    }

    @Test
    @DisplayName("sweep records a failure and never records a contribution when the charge fails")
    void sweep_recordsFailure_onDeclinedCharge() {
        DueAutoDebitMandate due = new DueAutoDebitMandate(MANDATE_ID, MEMBER_ID, AMOUNT, "AUTH_1");
        when(mandateLookup.findDueMandates(any(LocalDate.class))).thenReturn(List.of(due));
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.of(new MemberSummary(MEMBER_ID, "chidi@example.com")));
        when(paystackChargeClient.chargeAuthorization("AUTH_1", AMOUNT, "chidi@example.com"))
                .thenReturn(new PaystackChargeResult(false, null, AMOUNT, "Insufficient funds"));

        service.sweep();

        verify(contributionRecorder, never()).recordPaystackContribution(any(), any(), any());
        verify(mandateRecorder).recordChargeFailure(MANDATE_ID, "Insufficient funds");
    }

    @Test
    @DisplayName("sweep records a failure when the member behind the mandate can no longer be found")
    void sweep_recordsFailure_whenMemberMissing() {
        DueAutoDebitMandate due = new DueAutoDebitMandate(MANDATE_ID, MEMBER_ID, AMOUNT, "AUTH_1");
        when(mandateLookup.findDueMandates(any(LocalDate.class))).thenReturn(List.of(due));
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.empty());

        service.sweep();

        verify(paystackChargeClient, never()).chargeAuthorization(any(), any(), any());
        verify(mandateRecorder).recordChargeFailure(eq(MANDATE_ID), any());
    }
}
