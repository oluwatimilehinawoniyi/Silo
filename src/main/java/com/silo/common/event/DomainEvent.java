package com.silo.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base type for every fact published across module boundaries via
 * {@link org.springframework.context.ApplicationEventPublisher}.
 * <p>
 * Publish from inside the {@code @Transactional} method that persisted the
 * originating record - listeners use {@code @TransactionalEventListener},
 * which only fires after that transaction commits, and is silently skipped
 * if there is no active transaction at publish time.
 */
public abstract class DomainEvent {

    private final UUID eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
