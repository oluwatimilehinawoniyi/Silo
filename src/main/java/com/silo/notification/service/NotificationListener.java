package com.silo.notification.service;

import com.silo.auth.OfficerLookup;
import com.silo.auth.event.OfficerApplicationSubmittedEvent;
import com.silo.contribution.event.AutoDebitChargeFailedEvent;
import com.silo.loan.event.GuarantorInvitedEvent;
import com.silo.loan.event.GuarantorLiabilityAllocation;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.repayment.event.RepaymentMadeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final OfficerLookup officerLookup;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanApproved(LoanApprovedEvent event) {
        notificationService.notify(
                event.getMemberId(),
                LoanApprovedEvent.class.getSimpleName(),
                "Your loan has been approved",
                "Loan " + event.getLoanId() + " for " + event.getPrincipalAmount() + " has been approved and disbursed.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGuarantorInvited(GuarantorInvitedEvent event) {
        notificationService.notify(
                event.getGuarantorMemberId(),
                GuarantorInvitedEvent.class.getSimpleName(),
                "You've been invited to guarantee a loan",
                "You've been asked to guarantee loan request " + event.getLoanRequestId() + ".");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRepaymentMade(RepaymentMadeEvent event) {
        String subject = event.isLiabilityPayment() ? "Guarantor liability repayment received" : "Repayment received";
        notificationService.notify(
                event.getPayerMemberId(),
                RepaymentMadeEvent.class.getSimpleName(),
                subject,
                "We've received a repayment of " + event.getAmount() + " for loan " + event.getLoanId() + ".");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanDefaulted(LoanDefaultedEvent event) {
        notificationService.notify(
                event.getMemberId(),
                LoanDefaultedEvent.class.getSimpleName(),
                "Your loan has defaulted",
                "Loan " + event.getLoanId() + " has defaulted with an outstanding balance of "
                        + event.getOutstandingBalance() + ".");

        for (GuarantorLiabilityAllocation allocation : event.getGuarantorLiabilityAllocations()) {
            notificationService.notify(
                    allocation.getGuarantorMemberId(),
                    LoanDefaultedEvent.class.getSimpleName(),
                    "You've been assigned a guarantor liability",
                    "Loan " + event.getLoanId() + " defaulted; you've been assigned a liability of "
                            + allocation.getAmount() + ".");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOfficerApplicationSubmitted(OfficerApplicationSubmittedEvent event) {
        for (UUID officerId : officerLookup.findAllOfficerMemberIds()) {
            notificationService.notify(
                    officerId,
                    OfficerApplicationSubmittedEvent.class.getSimpleName(),
                    "New officer application awaiting your review",
                    "Member " + event.getApplicantMemberId() + " has applied to become an officer. "
                            + "Two distinct officer approvals are required.");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAutoDebitChargeFailed(AutoDebitChargeFailedEvent event) {
        String subject = event.isMandateStopped()
                ? "Auto-debit stopped after repeated failures"
                : "Auto-debit payment failed";
        String body = event.isMandateStopped()
                ? "We couldn't charge your card for " + event.getAmount() + " (" + event.getReason() + "). "
                        + "Auto-debit has been stopped - set it up again once you've resolved the issue with your card."
                : "We couldn't charge your card for " + event.getAmount() + " (" + event.getReason() + "). "
                        + "We'll try again next period.";
        notificationService.notify(event.getMemberId(), AutoDebitChargeFailedEvent.class.getSimpleName(), subject, body);
    }
}
