package com.silo.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.common.event.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

    @Mock
    private OutboxEventRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = OutboxJacksonConfig.create();

    @Test
    void republishesTheEventAndMarksTheRowDispatched() {
        OutboxEventProcessor processor = new OutboxEventProcessor(repository, eventPublisher, objectMapper);
        TestEvent original = new TestEvent();
        OutboxEvent row = toOutboxRow(original);

        processor.dispatch(row);

        ArgumentCaptor<TestEvent> republished = ArgumentCaptor.forClass(TestEvent.class);
        verify(eventPublisher).publishEvent(republished.capture());
        // Must round-trip the same eventId, or OutboxEventWriter's idempotency
        // guard won't recognize the redelivery and will insert a duplicate row.
        assertThat(republished.getValue().getEventId()).isEqualTo(original.getEventId());
        verify(repository).save(row);
        assertThat(row.getStatus()).isEqualTo(OutboxEventStatus.DISPATCHED);
        assertThat(row.getDispatchedAt()).isNotNull();
    }

    @Test
    void wrapsAnUnresolvableEventTypeInOutboxException() {
        OutboxEventProcessor processor = new OutboxEventProcessor(repository, eventPublisher, objectMapper);
        OutboxEvent row = new OutboxEvent(UUID.randomUUID(), "com.silo.does.not.Exist", "{}", Instant.now());

        assertThatThrownBy(() -> processor.dispatch(row))
                .isInstanceOf(OutboxException.class)
                .hasMessageContaining(row.getId().toString());
    }

    private OutboxEvent toOutboxRow(DomainEvent event) {
        try {
            return new OutboxEvent(event.getEventId(), event.getClass().getName(),
                    objectMapper.writeValueAsString(event), event.getOccurredAt());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    static class TestEvent extends DomainEvent {
    }
}
