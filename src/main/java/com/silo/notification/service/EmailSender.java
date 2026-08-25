package com.silo.notification.service;

/**
 * The actual delivery mechanism is swappable without touching any listener -
 * only this interface's boundary matters to NotificationService.
 */
public interface EmailSender {

    void send(String toEmail, String subject, String body) throws EmailDeliveryException;
}
