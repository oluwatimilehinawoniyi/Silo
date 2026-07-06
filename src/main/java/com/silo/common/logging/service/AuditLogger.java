package com.silo.common.logging.service;

import com.silo.common.logging.model.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j(topic = "AUDIT")
@Service
public class AuditLogger {

    public void log(AuditEvent event) {

        if (event == null) {
            return;
        }

        log.info(
                "Audit event",
                keyValue("correlationId", event.getCorrelationId()),
                keyValue("actorId", event.getActorId()),
                keyValue("actorType", event.getActorType()),
                keyValue("module", event.getModule()),
                keyValue("action", event.getAction()),
                keyValue("resource", event.getResource()),
                keyValue("resourceId", event.getResourceId()),
                keyValue("amount", event.getAmount()),
                keyValue("currency", event.getCurrency()),
                keyValue("reference", event.getReference()),
                keyValue("metadata", event.getMetadata()),
                keyValue("timestamp", event.getTimestamp())
        );
    }
}