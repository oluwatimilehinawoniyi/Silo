package com.silo.notification.service;

import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import com.silo.notification.dto.NotificationLogResponse;
import com.silo.notification.entity.NotificationLog;
import com.silo.notification.enums.NotificationStatus;
import com.silo.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private EmailSender emailSender;

    private NotificationService notificationService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String EMAIL = "member@example.com";

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(2);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());

        notificationService = new NotificationService(
                notificationLogRepository, memberLookup, emailSender, retryTemplate);
    }

    @Test
    @DisplayName("notify logs SENT when the member exists and delivery succeeds")
    void notify_logsSent_whenDeliverySucceeds() throws EmailDeliveryException {
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.of(new MemberSummary(MEMBER_ID, EMAIL)));

        notificationService.notify(MEMBER_ID, "LoanApprovedEvent", "Subject", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(emailSender, times(1)).send(EMAIL, "Subject", "Body");
    }

    @Test
    @DisplayName("notify logs FAILED when the member doesn't exist, without attempting delivery")
    void notify_logsFailed_whenMemberNotFound() throws EmailDeliveryException {
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.empty());

        notificationService.notify(MEMBER_ID, "LoanApprovedEvent", "Subject", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailSender, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("notify retries on delivery failure and logs FAILED once retries are exhausted")
    void notify_retriesThenLogsFailed_whenDeliveryKeepsFailing() throws EmailDeliveryException {
        when(memberLookup.findById(MEMBER_ID)).thenReturn(Optional.of(new MemberSummary(MEMBER_ID, EMAIL)));
        doThrow(new EmailDeliveryException("boom", null)).when(emailSender).send(EMAIL, "Subject", "Body");

        notificationService.notify(MEMBER_ID, "LoanApprovedEvent", "Subject", "Body");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(emailSender, times(2)).send(EMAIL, "Subject", "Body");
    }

    @Test
    @DisplayName("getHistory maps the repository results")
    void getHistory_mapsResults() {
        NotificationLog log = NotificationLog.builder()
                .id(UUID.randomUUID()).memberId(MEMBER_ID).eventType("LoanApprovedEvent")
                .channel(com.silo.notification.enums.NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT).sentAt(LocalDateTime.now()).build();
        when(notificationLogRepository.findByMemberIdOrderBySentAtDesc(MEMBER_ID)).thenReturn(List.of(log));

        List<NotificationLogResponse> history = notificationService.getHistory(MEMBER_ID);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).memberId()).isEqualTo(MEMBER_ID);
    }
}
