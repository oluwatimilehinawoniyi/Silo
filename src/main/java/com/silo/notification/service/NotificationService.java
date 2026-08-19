package com.silo.notification.service;

import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import com.silo.notification.dto.NotificationLogResponse;
import com.silo.notification.entity.NotificationLog;
import com.silo.notification.enums.NotificationChannel;
import com.silo.notification.enums.NotificationStatus;
import com.silo.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final MemberLookup memberLookup;
    private final EmailSender emailSender;
    private final RetryTemplate outboundCallRetryTemplate;

    @Transactional
    public void notify(UUID memberId, String eventType, String subject, String body) {
        Optional<MemberSummary> member = memberLookup.findById(memberId);
        NotificationStatus status = member.isPresent()
                ? attemptSend(member.get().email(), subject, body)
                : NotificationStatus.FAILED;

        if (member.isEmpty()) {
            log.warn("Cannot send {} notification: member {} not found", eventType, memberId);
        }

        notificationLogRepository.save(NotificationLog.builder()
                .memberId(memberId)
                .eventType(eventType)
                .subject(subject)
                .channel(NotificationChannel.EMAIL)
                .status(status)
                .sentAt(LocalDateTime.now())
                .build());
    }

    public List<NotificationLogResponse> getHistory(UUID memberId) {
        return notificationLogRepository.findByMemberIdOrderBySentAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationStatus attemptSend(String toEmail, String subject, String body) {
        try {
            outboundCallRetryTemplate.execute(context -> {
                emailSender.send(toEmail, subject, body);
                return null;
            });
            return NotificationStatus.SENT;
        } catch (EmailDeliveryException ex) {
            log.error("Email delivery permanently failed to {}: {}", toEmail, ex.getMessage());
            return NotificationStatus.FAILED;
        }
    }

    private NotificationLogResponse toResponse(NotificationLog notificationLog) {
        return new NotificationLogResponse(
                notificationLog.getId(), notificationLog.getMemberId(), notificationLog.getEventType(),
                notificationLog.getSubject(), notificationLog.getChannel(), notificationLog.getStatus(),
                notificationLog.getSentAt());
    }
}
