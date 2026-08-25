package com.silo.notification.dto;

import com.silo.notification.enums.NotificationChannel;
import com.silo.notification.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationLogResponse(
        UUID id,
        UUID memberId,
        String eventType,
        String subject,
        NotificationChannel channel,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
}
