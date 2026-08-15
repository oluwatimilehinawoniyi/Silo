package com.silo.paymentgateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.silo.paymentgateway.dto.PaystackWebhookPayload;
import com.silo.paymentgateway.service.PaystackSignatureVerifier;
import com.silo.paymentgateway.service.PaystackTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/webhooks/paystack")
@Tag(name = "Paystack Webhook", description = "Intake point for Paystack payment notifications")
@Slf4j
public class PaystackWebhookController {

    private static final String CHARGE_SUCCESS_EVENT = "charge.success";
    private static final BigDecimal KOBO_PER_NAIRA = BigDecimal.valueOf(100);

    private final PaystackSignatureVerifier signatureVerifier;
    private final PaystackTransactionService paystackTransactionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaystackWebhookController(
            PaystackSignatureVerifier signatureVerifier, PaystackTransactionService paystackTransactionService) {
        this.signatureVerifier = signatureVerifier;
        this.paystackTransactionService = paystackTransactionService;
    }

    @PostMapping
    @Operation(
            summary = "Paystack webhook receiver",
            description = "Every call is signature-verified before being trusted. Always acknowledges a validly "
                    + "signed call, regardless of whether the payload could be attributed to a member.")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected Paystack webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaystackWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, PaystackWebhookPayload.class);
        } catch (JsonProcessingException ex) {
            log.warn("Rejected malformed Paystack webhook payload: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (!CHARGE_SUCCESS_EVENT.equals(payload.event()) || payload.data() == null) {
            return ResponseEntity.ok().build();
        }

        BigDecimal amountInNaira = BigDecimal.valueOf(payload.data().amount()).divide(KOBO_PER_NAIRA);
        String customerEmail = payload.data().customer() != null ? payload.data().customer().email() : null;

        if (customerEmail == null) {
            log.warn("Paystack webhook missing customer email, reference={}", payload.data().reference());
            return ResponseEntity.ok().build();
        }

        paystackTransactionService.processVerifiedTransaction(
                payload.data().reference(), amountInNaira, customerEmail);

        return ResponseEntity.ok().build();
    }
}
