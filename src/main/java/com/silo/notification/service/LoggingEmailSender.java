package com.silo.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default EmailSender: logs instead of delivering. Active unless
 * silo.email.provider is set to a real provider - swapping providers means
 * changing that one property, nothing else.
 */
@Component
@ConditionalOnProperty(name = "silo.email.provider", havingValue = "log", matchIfMissing = true)
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("Email to {} - subject: \"{}\"", toEmail, subject);
    }
}
