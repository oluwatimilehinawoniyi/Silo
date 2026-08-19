package com.silo.notification.service;

import com.silo.auth.OfficerLookup;
import com.silo.auth.event.OfficerApplicationSubmittedEvent;
import com.silo.contribution.event.AutoDebitChargeFailedEvent;
import com.silo.loan.event.GuarantorInvitedEvent;
import com.silo.loan.event.GuarantorLiabilityAllocation;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.member.event.MemberRegisteredEvent;
import com.silo.repayment.event.RepaymentMadeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OfficerLookup officerLookup;

    @InjectMocks
    private NotificationListener listener;

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("onLoanApproved notifies the borrower")
    void onLoanApproved_notifiesBorrower() {
        LoanApprovedEvent event = new LoanApprovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), MEMBER_ID,
                new BigDecimal("5000"), LocalDateTime.now());

        listener.onLoanApproved(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("LoanApprovedEvent"), any(), any());
    }

    @Test
    @DisplayName("onGuarantorInvited notifies the invited guarantor")
    void onGuarantorInvited_notifiesGuarantor() {
        GuarantorInvitedEvent event =
                new GuarantorInvitedEvent(UUID.randomUUID(),
                        UUID.randomUUID(), MEMBER_ID);

        listener.onGuarantorInvited(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("GuarantorInvitedEvent"), any(), any());
    }

    @Test
    @DisplayName("onRepaymentMade notifies the payer")
    void onRepaymentMade_notifiesPayer() {
        RepaymentMadeEvent event = new RepaymentMadeEvent(
                UUID.randomUUID(), UUID.randomUUID(), MEMBER_ID,
                new BigDecimal("500"), null);

        listener.onRepaymentMade(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("RepaymentMadeEvent"), any(), any());
    }

    @Test
    @DisplayName(
            "onLoanDefaulted notifies the borrower and every assigned guarantor")
    void onLoanDefaulted_notifiesBorrowerAndGuarantors() {
        UUID guarantorId = UUID.randomUUID();
        LoanDefaultedEvent event = new LoanDefaultedEvent(
                UUID.randomUUID(), MEMBER_ID, new BigDecimal("10000"),
                List.of(new GuarantorLiabilityAllocation(UUID.randomUUID(),
                        guarantorId, new BigDecimal("10000"))));

        listener.onLoanDefaulted(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("LoanDefaultedEvent"), any(), any());
        verify(notificationService).notify(eq(guarantorId),
                eq("LoanDefaultedEvent"), any(), any());
    }

    @Test
    @DisplayName(
            "onOfficerApplicationSubmitted notifies every current officer")
    void onOfficerApplicationSubmitted_notifiesEveryOfficer() {
        UUID officerOne = UUID.randomUUID();
        UUID officerTwo = UUID.randomUUID();
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(
                List.of(officerOne, officerTwo));
        OfficerApplicationSubmittedEvent event =
                new OfficerApplicationSubmittedEvent(UUID.randomUUID(),
                        MEMBER_ID);

        listener.onOfficerApplicationSubmitted(event);

        verify(notificationService).notify(eq(officerOne),
                eq("OfficerApplicationSubmittedEvent"), any(), any());
        verify(notificationService).notify(eq(officerTwo),
                eq("OfficerApplicationSubmittedEvent"), any(), any());
    }

    @Test
    @DisplayName("onMemberRegistered notifies the new member")
    void onMemberRegistered_notifiesNewMember() {
        MemberRegisteredEvent event = new MemberRegisteredEvent(MEMBER_ID);

        listener.onMemberRegistered(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("MemberRegisteredEvent"), any(), any());
    }

    @Test
    @DisplayName(
            "onAutoDebitChargeFailed notifies the member on every failure")
    void onAutoDebitChargeFailed_notifiesMember() {
        AutoDebitChargeFailedEvent event = new AutoDebitChargeFailedEvent(
                UUID.randomUUID(), MEMBER_ID, new BigDecimal("2000"),
                "Insufficient funds", false);

        listener.onAutoDebitChargeFailed(event);

        verify(notificationService).notify(eq(MEMBER_ID),
                eq("AutoDebitChargeFailedEvent"), any(), any());
    }
}
