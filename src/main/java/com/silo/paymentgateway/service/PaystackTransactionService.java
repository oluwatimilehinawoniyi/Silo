package com.silo.paymentgateway.service;

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
}