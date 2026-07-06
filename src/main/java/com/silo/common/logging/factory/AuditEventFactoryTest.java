package com.silo.common.logging.factory;

import com.silo.common.logging.enums.AuditAction;
import com.silo.common.logging.enums.AuditModule;
import com.silo.common.logging.enums.AuditResource;
import com.silo.common.logging.model.AuditEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuditEventFactoryTest {

    private final AuditEventFactory factory = new AuditEventFactory();

    @Test
    void should_create_contribution_audit_event() {

        AuditEvent event = factory.contributionRecorded(
                "SYSTEM",
                "CONTRIB-1",
                BigDecimal.valueOf(1000),
                "GNF",
                Map.of("channel", "paystack")
        );

        assertEquals(AuditAction.CONTRIBUTION_RECORDED, event.getAction());
        assertEquals(AuditModule.CONTRIBUTION, event.getModule());
        assertEquals(AuditResource.CONTRIBUTION, event.getResource());
        assertEquals("CONTRIB-1", event.getResourceId());
        assertEquals(BigDecimal.valueOf(1000), event.getAmount());
    }
}