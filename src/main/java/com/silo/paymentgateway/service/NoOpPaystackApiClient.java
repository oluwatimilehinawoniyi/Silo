package com.silo.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Default PaystackApiClient: no real HTTP integration yet, so there's
 * nothing to reconcile against. Stands in until Paystack's List
 * Transactions API is actually wired up - swapping the bean is the only
 * change needed once it is.
 */
@Component
@Slf4j
public class NoOpPaystackApiClient implements PaystackApiClient {

    @Override
    public List<PaystackApiTransaction> listSuccessfulTransactionsSince(Instant since) {
        log.info("Paystack API integration not yet configured; skipping reconciliation lookup since {}", since);
        return List.of();
    }
}
