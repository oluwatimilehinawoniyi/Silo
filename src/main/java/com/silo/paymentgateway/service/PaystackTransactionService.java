package com.silo.paymentgateway.service;

import com.silo.contribution.ContributionRecorder;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import com.silo.paymentgateway.entity.PaystackTransaction;
import com.silo.paymentgateway.entity.PaystackTransactionStatus;
import com.silo.paymentgateway.repository.PaystackTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackTransactionService {

    private final PaystackTransactionRepository repository;
    private final MemberLookup memberLookup;
    private final ContributionRecorder contributionRecorder;


    @Transactional
    public Optional<PaystackTransaction> recordIfNew(String paystackReference, BigDecimal amount, UUID memberId) {
        if (repository.existsByPaystackReference(paystackReference)) {
            log.info("Ignoring duplicate Paystack webhook, reference={}", paystackReference);
            return Optional.empty();
        }

        PaystackTransaction transaction = PaystackTransaction.builder()
                .paystackReference(paystackReference)
                .amount(amount)
                .memberId(memberId)
                .status(PaystackTransactionStatus.RECEIVED)
                .build();

        try {
            return Optional.of(repository.save(transaction));
        } catch (DataIntegrityViolationException duplicateRace) {
            // Two near-simultaneous deliveries of the same reference both passed the
            // exists() check above; the unique constraint is the final safety net.
            log.info("Duplicate Paystack webhook caught by unique constraint, reference={}", paystackReference);
            return Optional.empty();
        }
    }

    /** Marks a transaction as fully processed once its ContributionMadeEvent has been published. */
    @Transactional
    public void markProcessed(UUID transactionId) {
        repository.findById(transactionId).ifPresent(transaction -> {
            transaction.setStatus(PaystackTransactionStatus.PROCESSED);
            transaction.setProcessedAt(Instant.now());
        });
    }

    /** Marks a transaction as failed - Paystack reported it as unsuccessful. */
    @Transactional
    public void markFailed(UUID transactionId) {
        repository.findById(transactionId)
                .ifPresent(transaction -> transaction.setStatus(PaystackTransactionStatus.FAILED));
    }

    /**
     * The shared core of "a Paystack payment was verified" - used by both the webhook
     * receiver and the reconciliation sweep. Resolves the paying member, records the
     * dedup row if new, then asks Contribution to record it. Returns true only when
     * this call newly recorded a contribution (false for duplicates, unknown members,
     * or a downstream failure).
     */
    @Transactional
    public boolean processVerifiedTransaction(String paystackReference, BigDecimal amount, String customerEmail) {
        Optional<MemberSummary> member = memberLookup.findByEmail(customerEmail);
        if (member.isEmpty()) {
            log.warn("Cannot attribute Paystack payment, reference={}, no member with email {}",
                    paystackReference, customerEmail);
            return false;
        }

        Optional<PaystackTransaction> transaction = recordIfNew(paystackReference, amount, member.get().id());
        if (transaction.isEmpty()) {
            return false;
        }

        try {
            contributionRecorder.recordPaystackContribution(member.get().id(), amount, paystackReference);
            markProcessed(transaction.get().getId());
            return true;
        } catch (RuntimeException ex) {
            log.error("Failed to record contribution for Paystack reference={}: {}",
                    paystackReference, ex.getMessage());
            markFailed(transaction.get().getId());
            return false;
        }
    }
}
