package com.silo.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.common.event.DomainEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Keeps {@code event_outbox} in sync with the lifecycle of every published
 * {@link DomainEvent}:
 * <p>
 * 1. BEFORE_COMMIT - durably record it as PENDING in the same transaction as
 * its originating write, so a crash before commit can't lose it.
 * <p>
 * 2. AFTER_COMMIT - mark it DISPATCHED immediately once the original commit's
 * in-process listeners have had their chance to run. This keeps the happy
 * path to a single delivery; {@link OutboxDispatcher} only ever finds rows
 * still PENDING here because the process crashed between the two phases
 * above, or a listener threw and interrupted this callback from running.
 */
@Component
public class OutboxEventWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(OutboxEventRepository repository,
                              @Qualifier("outboxObjectMapper") ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void writeToOutbox(DomainEvent event) {
        // Redelivery re-publishes the same event to re-trigger AFTER_COMMIT
        // module listeners; skip it here or it would reset the row back to PENDING.
        if (repository.existsById(event.getEventId())) {
            return;
        }
        repository.save(new OutboxEvent(event.getEventId(), event.getClass().getName(), serialize(event),
                event.getOccurredAt()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDelivered(DomainEvent event) {
        repository.findById(event.getEventId()).ifPresent(row -> {
            row.markDispatched(Instant.now());
            repository.save(row);
        });
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new OutboxException("Failed to serialize event " + event.getEventId(), ex);
        }
    }
}
