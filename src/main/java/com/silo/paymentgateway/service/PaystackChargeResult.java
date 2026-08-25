package com.silo.paymentgateway.service;

import java.math.BigDecimal;

public record PaystackChargeResult(boolean success, String reference, BigDecimal amount, String failureReason) {
}
