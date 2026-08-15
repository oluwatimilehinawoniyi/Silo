package com.silo.paymentgateway.service;

import java.math.BigDecimal;
import java.time.Instant;

public record PaystackApiTransaction(String reference, BigDecimal amount, String customerEmail, Instant paidAt) {
}
