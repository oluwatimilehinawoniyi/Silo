package com.silo.common.outbox;

import com.silo.common.event.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the full outbox round trip against a real Postgres: an event
 * published inside a committed transaction lands in event_outbox as
 * PENDING, then gets marked DISPATCHED as soon as the same commit's
 * in-process listeners run - so a healthy happy path delivers exactly once,
 * and OutboxDispatcher's poll only ever redelivers rows a crash left stuck
 * as PENDING.
 */
@SpringBootTest(properties = {
        "silo.outbox.poll-interval-ms=600000",
        "silo.outbox.poll-initial-delay-ms=600000"
})
@Testcontainers
class OutboxIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SampleEventPublisher publisher;

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxDispatcher dispatcher;

    @Autowired
    private SampleEventListener listener;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        listener.received.clear();
    }

    @Test
    void happyPathDeliversOnceAndMarksTheRowDispatchedWithoutPolling() {
        UUID eventId = publisher.publishSampleEvent();

        OutboxEvent row = repository.findById(eventId).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxEventStatus.DISPATCHED);
        assertThat(row.getDispatchedAt()).isNotNull();
        assertThat(listener.received).hasSize(1);
        assertThat(listener.received.get(0).getEventId()).isEqualTo(eventId);

        dispatcher.pollAndDispatch();

        // Already dispatched by the original commit - the poller must not
        // find or redeliver it again.
        assertThat(listener.received).hasSize(1);
    }

    @Test
    void pollerRedeliversARowLeftPendingAsIfTheProcessHadCrashed() throws Exception {
        // Simulates a crash between the BEFORE_COMMIT write and the
        // AFTER_COMMIT mark-delivered callback: a row that's durably
        // recorded but never got marked DISPATCHED.
        SampleEvent event = new SampleEvent();
        String payload = OutboxJacksonConfig.create().writeValueAsString(event);
        repository.save(new OutboxEvent(event.getEventId(), event.getClass().getName(), payload,
                event.getOccurredAt()));

        dispatcher.pollAndDispatch();

        OutboxEvent row = repository.findById(event.getEventId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(OutboxEventStatus.DISPATCHED);
        assertThat(listener.received).hasSize(1);
        assertThat(listener.received.get(0).getEventId()).isEqualTo(event.getEventId());
    }

    static class SampleEvent extends DomainEvent {
    }

    static class SampleEventPublisher {

        private final ApplicationEventPublisher eventPublisher;

        SampleEventPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        UUID publishSampleEvent() {
            SampleEvent event = new SampleEvent();
            eventPublisher.publishEvent(event);
            return event.getEventId();
        }
    }

    static class SampleEventListener {

        final List<SampleEvent> received = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void on(SampleEvent event) {
            received.add(event);
        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        SampleEventPublisher sampleEventPublisher(ApplicationEventPublisher eventPublisher) {
            return new SampleEventPublisher(eventPublisher);
        }

        @Bean
        SampleEventListener sampleEventListener() {
            return new SampleEventListener();
        }
    }
}
