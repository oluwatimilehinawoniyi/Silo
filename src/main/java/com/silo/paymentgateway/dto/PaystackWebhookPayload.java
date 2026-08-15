package com.silo.paymentgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackWebhookPayload(String event, PaystackWebhookData data) {
}
