package com.silo.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Real email delivery via Resend's HTTP API. Active only when
 * silo.email.provider=resend - LoggingEmailSender stays the default so a
 * misconfigured or missing API key never silently blocks the app.
 */
@Component
@ConditionalOnProperty(name = "silo.email.provider", havingValue = "resend")
@Slf4j
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;

    public ResendEmailSender(
            @Value("${silo.resend.api-key}") String apiKey,
            @Value("${silo.resend.from-email}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.restClient = RestClient.create("https://api.resend.com");
    }

    @Override
    public void send(String toEmail, String subject, String body) throws EmailDeliveryException {
        Map<String, Object> requestBody = Map.of(
                "from", fromEmail,
                "to", List.of(toEmail),
                "subject", subject,
                "text", body);

        try {
            restClient.post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new EmailDeliveryException(
                    "Resend rejected the email (" + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new EmailDeliveryException("Resend request failed: " + ex.getMessage(), ex);
        }
    }
}
