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
                compose("Good news - your loan has been approved and the funds are on their way.\n\n"
                        + "Amount disbursed: " + event.getPrincipalAmount() + "\n"
                        + "Loan reference: " + event.getLoanId() + "\n\n"
                        + "You can view your repayment schedule any time from your Silo dashboard. "
                        + "If anything looks off, reach out to your cooperative's office."));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGuarantorInvited(GuarantorInvitedEvent event) {
        notificationService.notify(
                event.getGuarantorMemberId(),
                GuarantorInvitedEvent.class.getSimpleName(),
                "You've been asked to guarantee a loan",
                compose("A fellow member has asked you to stand as guarantor on their loan request.\n\n"
                        + "Loan request reference: " + event.getLoanRequestId() + "\n\n"
                        + "Being a guarantor means you're agreeing to help cover this loan if the borrower is "
                        + "unable to repay it, so take a moment to review the details before you decide. "
                        + "You can accept or decline from your Silo dashboard - there's no rush, but the "
                        + "borrower is waiting on your response before their request can move forward."));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRepaymentMade(RepaymentMadeEvent event) {
        String subject = event.isLiabilityPayment() ? "Your guarantor payment was received" : "Repayment received";
        String context = event.isLiabilityPayment()
                ? "Thank you for covering this liability on behalf of the borrower - it's been applied to the loan."
                : "Thank you for staying on top of your repayments - it's been applied to your loan balance.";
        notificationService.notify(
                event.getPayerMemberId(),
                RepaymentMadeEvent.class.getSimpleName(),
                subject,
                compose("We've received your payment of " + event.getAmount() + " for loan " + event.getLoanId()
                        + ".\n\n" + context + " You can check your updated balance any time from your dashboard."));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanDefaulted(LoanDefaultedEvent event) {
        notificationService.notify(
                event.getMemberId(),
                LoanDefaultedEvent.class.getSimpleName(),
                "Your loan is now in default",
                compose("Loan " + event.getLoanId() + " has missed enough consecutive repayments to be marked "
                        + "as defaulted. The outstanding balance is " + event.getOutstandingBalance() + ".\n\n"
                        + "Your guarantors are being notified and may begin covering this balance on your "
                        + "behalf. We'd encourage you to reach out to your cooperative's office as soon as "
                        + "possible to discuss repayment - this affects your standing for future loans."));

        for (GuarantorLiabilityAllocation allocation : event.getGuarantorLiabilityAllocations()) {
            notificationService.notify(
                    allocation.getGuarantorMemberId(),
                    LoanDefaultedEvent.class.getSimpleName(),
                    "A loan you guaranteed has defaulted",
                    compose("A loan you guaranteed (reference " + event.getLoanId() + ") has defaulted, and "
                            + "you've been assigned a share of " + allocation.getAmount() + " to help cover it.\n\n"
                            + "You can settle this from your Silo dashboard whenever you're ready. If you have "
                            + "questions about how this was calculated, your cooperative's office can walk you "
                            + "through it."));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMemberRegistered(MemberRegisteredEvent event) {
        notificationService.notify(
                event.getMemberId(),
                MemberRegisteredEvent.class.getSimpleName(),
                "Welcome to Silo - let's set your password",
                compose("Your Silo member profile has been created - welcome to the cooperative!\n\n"
                        + "Your member ID is " + event.getMemberId() + ". You'll need it to set your password "
                        + "and log in for the first time. Once you're in, take a moment to complete your KYC "
                        + "details so you're ready to contribute and apply for loans.\n\n"
                        + "If you weren't expecting this email, you can safely ignore it."));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOfficerApplicationSubmitted(OfficerApplicationSubmittedEvent event) {
        for (UUID officerId : officerLookup.findAllOfficerMemberIds()) {
            notificationService.notify(
                    officerId,
                    OfficerApplicationSubmittedEvent.class.getSimpleName(),
                    "A new officer application is awaiting your review",
                    compose("Member " + event.getApplicantMemberId() + " has applied to become an officer.\n\n"
                            + "Elevating a member to officer requires two distinct officers to approve, so your "
                            + "review matters here. Please take a look from your dashboard when you get a "
                            + "chance and approve or reject the application."));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAutoDebitChargeFailed(AutoDebitChargeFailedEvent event) {
        String subject = event.isMandateStopped()
                ? "Your auto-debit has been stopped"
                : "We couldn't process your auto-debit contribution";
        String message = event.isMandateStopped()
                ? "We tried charging your card for your " + event.getAmount() + " auto-debit contribution, but "
                        + "it didn't go through (" + event.getReason() + "). After a few failed attempts, we've "
                        + "stopped auto-debit on this mandate so it doesn't keep failing quietly.\n\n"
                        + "No action was taken against your standing - you can set up auto-debit again any time "
                        + "once the issue with your card is resolved."
                : "We tried charging your card for your " + event.getAmount() + " auto-debit contribution, but "
                        + "it didn't go through (" + event.getReason() + ").\n\n"
                        + "We'll automatically try again next period, so there's nothing you need to do right "
                        + "now unless you'd like to update your card or contribution amount beforehand.";
        notificationService.notify(
                event.getMemberId(), AutoDebitChargeFailedEvent.class.getSimpleName(), subject, compose(message));
    }

    private String compose(String message) {
        return "Hi there,\n\n" + message + "\n\nWarm regards,\nThe Silo Team";
    }
}
