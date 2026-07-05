package com.silo.common.outbox;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A dedicated ObjectMapper for outbox payloads, isolated from the app-wide
 * Jackson config used for REST responses. DomainEvent subclasses are plain
 * immutable field-holders with no setters, so this reads/writes fields
 * directly (including private final ones) instead of requiring every event
 * to wire up a Jackson-friendly constructor.
 */
@Configuration
class OutboxJacksonConfig {

    @Bean
    ObjectMapper outboxObjectMapper() {
        return create();
    }

    static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
                .setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
                .setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
    }
}
