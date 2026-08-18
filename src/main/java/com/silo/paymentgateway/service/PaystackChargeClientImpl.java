package com.silo.paymentgateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
@Slf4j
class PaystackChargeClientImpl implements PaystackChargeClient {

    private static final BigDecimal KOBO_PER_NAIRA = BigDecimal.valueOf(100);

    private final RestClient restClient;
    private final String secretKey;

    PaystackChargeClientImpl(@Value("${silo.paystack.secret-key}") String secretKey) {
        this.secretKey = secretKey;
        this.restClient = RestClient.create("https://api.paystack.co");
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaystackChargeResult chargeAuthorization(String authorizationCode, BigDecimal amount, String email) {
        long amountInKobo = amount.multiply(KOBO_PER_NAIRA).setScale(0, RoundingMode.HALF_UP).longValueExact();

        Map<String, Object> requestBody = Map.of(
                "authorization_code", authorizationCode,
                "amount", amountInKobo,
                "email", email);

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/transaction/charge_authorization")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return new PaystackChargeResult(false, null, amount, (String) response.get("message"));
            }

            String status = (String) data.get("status");
            String reference = (String) data.get("reference");
            if ("success".equals(status)) {
                return new PaystackChargeResult(true, reference, amount, null);
            }

            String reason = (String) data.getOrDefault("gateway_response", "Charge was not successful");
            return new PaystackChargeResult(false, reference, amount, reason);
        } catch (Exception ex) {
            log.error("Paystack charge_authorization call failed: {}", ex.getMessage());
            return new PaystackChargeResult(false, null, amount, "Paystack request failed: " + ex.getMessage());
        }
    }
}
