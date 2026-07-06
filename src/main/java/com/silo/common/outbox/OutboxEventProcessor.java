package com.silo.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Redelivers a single outbox row by republishing its event, so every
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} module listener
 * fires again - then marks the row DISPATCHED in the same transaction. If
 * deserialization or a listener fails, the transaction rolls back and the
 * row stays PENDING for the next poll.
 */
@Component
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(OutboxEventRepository repository, ApplicationEventPublisher eventPublisher,
                                 @Qualifier("outboxObjectMapper") ObjectMapper objectMapper) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void dispatch(OutboxEvent outboxEvent) {
        DomainEvent event = deserialize(outboxEvent);
        eventPublisher.publishEvent(event);
        outboxEvent.markDispatched(Instant.now());
        repository.save(outboxEvent);
        log.info("Dispatched outbox event {} ({})", outboxEvent.getId(), outboxEvent.getEventType());
    }

    private DomainEvent deserialize(OutboxEvent outboxEvent) {
        try {
            Class<?> eventType = Class.forName(outboxEvent.getEventType());
            return (DomainEvent) objectMapper.readValue(outboxEvent.getPayload(), eventType);
        } catch (ClassNotFoundException | JsonProcessingException ex) {
            throw new OutboxException("Failed to deserialize outbox event " + outboxEvent.getId(), ex);
        }
    }
}
