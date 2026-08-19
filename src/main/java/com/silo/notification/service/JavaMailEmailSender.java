package com.silo.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

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
    private final String fromName;

    public JavaMailEmailSender(
            JavaMailSender mailSender,
            @Value("${silo.smtp.from-email}") String fromEmail,
            @Value("${silo.smtp.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String toEmail, String subject, String body) throws EmailDeliveryException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);

            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            throw new EmailDeliveryException("SMTP send failed: " + ex.getMessage(), ex);
        }
    }
}
