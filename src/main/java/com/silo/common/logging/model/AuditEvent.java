package com.silo.common.logging.model;

import com.silo.common.logging.enums.AuditAction;
import com.silo.common.logging.enums.AuditResource;
import com.silo.common.logging.enums.AuditModule;
import com.silo.common.logging.enums.AuditUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Value
@Builder
public class AuditEvent {

     String correlationId;

     String actorId;
     AuditUser actorType;   // MEMBER / OFFICER / SYSTEM

     AuditModule module;

     AuditAction action;

     AuditResource resource;
     String resourceId;

     BigDecimal amount;
     String currency;

    String reference;

    @Builder.Default
    Instant timestamp = Instant.now();

    @Builder.Default
     Map<String, String>
    metadata = new HashMap<>();
}