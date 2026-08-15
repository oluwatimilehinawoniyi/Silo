package com.silo.paymentgateway.service;

import com.silo.paymentgateway.repository.PaystackTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class PaystackReconciliationService {

    private final PaystackApiClient paystackApiClient;
    private final PaystackTransactionRepository paystackTransactionRepository;
    private final PaystackTransactionService paystackTransactionService;
    private final long lookbackHours;

    public PaystackReconciliationService(
            PaystackApiClient paystackApiClient,
            PaystackTransactionRepository paystackTransactionRepository,
            PaystackTransactionService paystackTransactionService,
            @Value("${silo.paystack.reconciliation-lookback-hours:48}") long lookbackHours) {
        this.paystackApiClient = paystackApiClient;
        this.paystackTransactionRepository = paystackTransactionRepository;
        this.paystackTransactionService = paystackTransactionService;
        this.lookbackHours = lookbackHours;
    }

    @Transactional
    public int reconcile() {
        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);
        int recovered = 0;

        for (PaystackApiTransaction remote : paystackApiClient.listSuccessfulTransactionsSince(since)) {
            if (paystackTransactionRepository.existsByPaystackReference(remote.reference())) {
                continue;
            }

            log.warn("Reconciliation found a Paystack transaction with no local record, reference={}",
                    remote.reference());
            boolean recorded = paystackTransactionService.processVerifiedTransaction(
                    remote.reference(), remote.amount(), remote.customerEmail());
            if (recorded) {
                recovered++;
            }
        }

        return recovered;
    }
}
