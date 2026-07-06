package com.silo.common.logging.factory;

import com.silo.common.logging.enums.*;
import com.silo.common.logging.model.AuditEvent;
import com.silo.common.logging.util.CorrelationIdUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
public class AuditEventFactory {

    private AuditEvent.AuditEventBuilder baseBuilder(String actorId,
                                                     AuditUser actorType,
                                                     AuditModule module) {

        return AuditEvent.builder()
                .correlationId(CorrelationIdUtil.getCorrelationId())
                .actorId(actorId)
                .actorType(actorType)
                .module(module)
                .timestamp(Instant.now());
    }
    /* =========================
       MEMBER
       ========================= */

    public AuditEvent memberRegistered(String actorId,String memberId, Map<String, String> metadata) {
        return baseBuilder(actorId, AuditUser.OFFICER, AuditModule.MEMBER)
                .action(AuditAction.MEMBER_REGISTERED)
                .resource(AuditResource.MEMBER)
                .resourceId(memberId)
                .metadata(metadata)
                .build();
    }

    public AuditEvent memberUpdated(String actorId, String memberId, Map<String, String> metadata) {
        return baseBuilder(actorId, AuditUser.OFFICER, AuditModule.MEMBER)
                .action(AuditAction.MEMBER_UPDATED)
                .resource(AuditResource.MEMBER)
                .resourceId(memberId)
                .metadata(metadata)
                .build();
    }

    public AuditEvent memberKycApproved(String actorId, String memberId) {
        return baseBuilder(actorId, AuditUser.OFFICER, AuditModule.MEMBER)
                .action(AuditAction.MEMBER_KYC_APPROVED)
                .resource(AuditResource.MEMBER)
                .resourceId(memberId)
                .build();
    }

    /* =========================
       CONTRIBUTION
       ========================= */

    public AuditEvent contributionRecorded(
            String actorId,
            String contributionId,
            BigDecimal amount,
            String currency,
            Map<String, String> metadata
    ) {
        return baseBuilder(actorId, AuditUser.MEMBER, AuditModule.CONTRIBUTION)
                .action(AuditAction.CONTRIBUTION_RECORDED)
                .resource(AuditResource.CONTRIBUTION)
                .resourceId(contributionId)
                .amount(amount)
                .currency(currency)
                .metadata(metadata)
                .build();
    }

    /* =========================
       LOAN
       ========================= */

    public AuditEvent loanRequested(String actorId, String loanRequestId) {
        return baseBuilder(actorId, AuditUser.MEMBER, AuditModule.LOAN)
                .action(AuditAction.LOAN_REQUESTED)
                .resource(AuditResource.LOAN_REQUEST)
                .resourceId(loanRequestId)
                .build();
    }

    public AuditEvent loanApproved(
            String actorId,
            AuditUser actorType,
            String loanId,
            BigDecimal amount,
            String currency
    ) {
        return baseBuilder(actorId, actorType, AuditModule.LOAN)
                .action(AuditAction.LOAN_APPROVED)
                .resource(AuditResource.LOAN)
                .resourceId(loanId)
                .amount(amount)
                .currency(currency)
                .build();
    }

    public AuditEvent loanDefaulted(String loanId, Map<String, String> metadata) {
        return baseBuilder("SYSTEM", AuditUser.SYSTEM, AuditModule.LOAN)
                .action(AuditAction.LOAN_DEFAULTED)
                .resource(AuditResource.LOAN)
                .resourceId(loanId)
                .metadata(metadata)
                .build();
    }

    /* =========================
       REPAYMENT
       ========================= */

    public AuditEvent repaymentRecorded(
            String actorId,
            String repaymentId,
            BigDecimal amount,
            String currency
    ) {
        return baseBuilder(actorId, AuditUser.MEMBER, AuditModule.REPAYMENT)
                .action(AuditAction.REPAYMENT_RECORDED)
                .resource(AuditResource.REPAYMENT)
                .resourceId(repaymentId)
                .amount(amount)
                .currency(currency)
                .build();
    }

    /* =========================
       PAYMENT GATEWAY
       ========================= */

    public AuditEvent paystackTransactionVerified(
            String actorId,
            String transactionRef,
            BigDecimal amount,
            String currency
    ) {
        return baseBuilder(actorId, AuditUser.SYSTEM, AuditModule.PAYMENT_GATEWAY)
                .action(AuditAction.PAYSTACK_TRANSACTION_VERIFIED)
                .resource(AuditResource.PAYSTACK_TRANSACTION)
                .resourceId(transactionRef)
                .amount(amount)
                .currency(currency)
                .reference(transactionRef)
                .build();
    }

    /* =========================
       NOTIFICATION
       ========================= */

    public AuditEvent notificationSent(
            String actorId,
            String notificationId,
            String channel,
            Map<String, String> metadata
    ) {
        return baseBuilder(actorId, AuditUser.SYSTEM, AuditModule.NOTIFICATION)
                .action(AuditAction.EMAIL_SENT)
                .resource(AuditResource.NOTIFICATION_LOG)
                .resourceId(notificationId)
                .metadata(metadata)
                .build();
    }

    /* =========================
       BASE BUILDER
       ========================= */


}