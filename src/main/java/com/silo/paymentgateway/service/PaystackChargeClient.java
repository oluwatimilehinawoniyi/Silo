package com.silo.paymentgateway.service;

import java.math.BigDecimal;

/**
 * Charges a previously-captured reusable authorization code - the
 * recurring-billing counterpart to the one-off checkout flow the webhook
 * handles. Swappable so tests never make a real Paystack call.
 */
public interface PaystackChargeClient {

    PaystackChargeResult chargeAuthorization(String authorizationCode, BigDecimal amount, String email);
}
