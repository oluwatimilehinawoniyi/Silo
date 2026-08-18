package com.silo.paymentgateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackAuthorization(
        @JsonProperty("authorization_code") String authorizationCode,
        boolean reusable
) {
}
