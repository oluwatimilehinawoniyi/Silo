package com.silo.common.logging.service;

import com.silo.common.logging.enums.AuditAction;
import com.silo.common.logging.enums.AuditModule;
import com.silo.common.logging.enums.AuditResource;
import com.silo.common.logging.enums.AuditUser;
import com.silo.common.logging.model.AuditEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuditLoggerTest {

    private final AuditLogger auditLogger = new AuditLogger();

    @Test
    void should_not_throw_exception_when_logging_event() {

        AuditEvent event = AuditEvent.builder()
                .actorId("SYSTEM")
                .actorType(AuditUser.SYSTEM)
                .module(AuditModule.COMMON)
                .action(AuditAction.LOGIN)
                .resource(AuditResource.MEMBER)
                .resourceId("TEST-1")
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(() -> auditLogger.log(event));
    }

    @Test
    void should_handle_null_safely() {
        assertDoesNotThrow(() -> auditLogger.log(null));
    }
}