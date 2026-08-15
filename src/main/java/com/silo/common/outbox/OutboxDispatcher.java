package com.silo.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls {@code event_outbox} for PENDING rows and redelivers each one via
 * {@link OutboxEventProcessor}. One row failing doesn't block the rest of
 * the batch - it's logged and retried on the next poll.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository repository;
    private final OutboxEventProcessor processor;

    public OutboxDispatcher(OutboxEventRepository repository, OutboxEventProcessor processor) {
        this.repository = repository;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${silo.outbox.poll-interval-ms:5000}",
            initialDelayString = "${silo.outbox.poll-initial-delay-ms:0}")
    public void pollAndDispatch() {
        Page<OutboxEvent> pending = repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent outboxEvent : pending) {
            try {
                processor.dispatch(outboxEvent);
            } catch (Exception ex) {
                log.error("Failed to dispatch outbox event {} ({})", outboxEvent.getId(), outboxEvent.getEventType(),
                        ex);
            }
        }
    }
}
