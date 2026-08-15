package com.silo.paymentgateway.service;

import java.time.Instant;
import java.util.List;

/**
 * Fetches successful charges from Paystack's own records, for the
 * reconciliation sweep to compare against what we have locally. Swappable
 * so the real HTTP integration can replace {@link NoOpPaystackApiClient}
 * without touching the reconciliation logic.
 */
public interface PaystackApiClient {

    List<PaystackApiTransaction> listSuccessfulTransactionsSince(Instant since);
}
