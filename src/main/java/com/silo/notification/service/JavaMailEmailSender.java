package com.silo.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real email delivery via plain SMTP (e.g. Gmail) - unlike Resend's shared
 * sandbox sender, a real SMTP account can send to any real address
 * immediately, no domain verification needed. Active only when
 * silo.email.provider=smtp.
 */
@Component
@ConditionalOnProperty(name = "silo.email.provider", havingValue = "smtp")
@Slf4j
public class JavaMailEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public JavaMailEmailSender(JavaMailSender mailSender,
                               @Value("${silo.smtp.from-email}")
                               String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String toEmail, String subject, String body)
            throws EmailDeliveryException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailDeliveryException(
                    "SMTP send failed: " + ex.getMessage(), ex);
        }
    }
}
