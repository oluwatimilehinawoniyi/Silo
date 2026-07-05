package com.silo.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.common.event.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventWriterTest {

    @Mock
    private OutboxEventRepository repository;

    private final ObjectMapper objectMapper = OutboxJacksonConfig.create();

    @Test
    void savesANewRowForAPreviouslyUnseenEvent() {
        OutboxEventWriter writer = new OutboxEventWriter(repository, objectMapper);
        TestEvent event = new TestEvent();
        when(repository.existsById(event.getEventId())).thenReturn(false);

        writer.writeToOutbox(event);

        verify(repository).save(any(OutboxEvent.class));
    }

    @Test
    void skipsWritingWhenTheEventIsAlreadyRecorded() {
        OutboxEventWriter writer = new OutboxEventWriter(repository, objectMapper);
        TestEvent event = new TestEvent();
        when(repository.existsById(event.getEventId())).thenReturn(true);

        writer.writeToOutbox(event);

        verify(repository, never()).save(any());
    }

    @Test
    void wrapsSerializationFailureInOutboxException() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(mock(JsonProcessingException.class));
        OutboxEventWriter writer = new OutboxEventWriter(repository, failingMapper);
        TestEvent event = new TestEvent();
        when(repository.existsById(event.getEventId())).thenReturn(false);

        assertThatThrownBy(() -> writer.writeToOutbox(event))
                .isInstanceOf(OutboxException.class)
                .hasMessageContaining(event.getEventId().toString());
    }

    static class TestEvent extends DomainEvent {
    }
}
