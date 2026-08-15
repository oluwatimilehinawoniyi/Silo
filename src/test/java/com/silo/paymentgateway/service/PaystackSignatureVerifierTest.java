package com.silo.paymentgateway.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class PaystackSignatureVerifierTest {

    private static final String SECRET = "test-paystack-secret";
    private static final String BODY = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"ref123\"}}";

    private final PaystackSignatureVerifier verifier = new PaystackSignatureVerifier(SECRET);

    @Test
    @DisplayName("isValid returns true for a correctly computed HMAC-SHA512 signature")
    void isValid_true_forCorrectSignature() throws Exception {
        String validSignature = computeSignature(SECRET, BODY);

        assertThat(verifier.isValid(BODY, validSignature)).isTrue();
    }

    @Test
    @DisplayName("isValid returns false for a mismatched signature")
    void isValid_false_forWrongSignature() {
        assertThat(verifier.isValid(BODY, "not-the-right-signature")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false for a null or blank signature header")
    void isValid_false_forMissingSignature() {
        assertThat(verifier.isValid(BODY, null)).isFalse();
        assertThat(verifier.isValid(BODY, "  ")).isFalse();
    }

    @Test
    @DisplayName("isValid returns false when the body was tampered with after signing")
    void isValid_false_whenBodyTampered() throws Exception {
        String validSignature = computeSignature(SECRET, BODY);

        assertThat(verifier.isValid(BODY + "tampered", validSignature)).isFalse();
    }

    private static String computeSignature(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
