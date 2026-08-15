package com.silo.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks a domain event as already applied to the reporting projections.
 * Unlike the ledger listeners (which are naturally idempotent since they
 * insert new rows keyed by transactionId), these listeners mutate running
 * counters in place, so outbox redelivery would double-count without this
 * explicit guard.
 */
@Entity
@Table(name = "reporting_processed_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportingProcessedEvent {

    @Id
    private UUID eventId;

    @Column(nullable = false)
    private Instant processedAt;
}
