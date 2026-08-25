package com.silo.paymentgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Paystack signs each webhook body with HMAC-SHA512 using your secret key,
 * sent in the x-paystack-signature header. Every call must be verified
 * against this before being trusted - an unsigned or mismatched call is
 * never processed, matching the doc's explicit business rule.
 */
@Component
public class PaystackSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA512";

    private final SecretKeySpec keySpec;

    public PaystackSignatureVerifier(@Value("${silo.paystack.secret-key}") String secretKey) {
        this.keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public boolean isValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);

            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute Paystack signature", ex);
        }
    }
}
