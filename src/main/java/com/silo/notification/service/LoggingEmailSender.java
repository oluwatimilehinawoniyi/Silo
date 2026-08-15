package com.silo.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default EmailSender: logs instead of delivering. Stands in until a real
 * provider (SES, SendGrid, etc.) is wired up - swapping it out means
 * providing a different EmailSender bean, nothing else changes.
 */
@Component
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("Email to {} - subject: \"{}\"", toEmail, subject);
    }
}
