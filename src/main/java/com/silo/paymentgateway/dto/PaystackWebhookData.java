package com.silo.paymentgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackWebhookData(
        String reference, Long amount, PaystackCustomer customer, PaystackAuthorization authorization) {
}
