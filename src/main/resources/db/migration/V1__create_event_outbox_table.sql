CREATE TABLE event_outbox (
    id            UUID PRIMARY KEY,
    event_type    VARCHAR(255) NOT NULL,
    payload       TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    dispatched_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_event_outbox_status_created_at ON event_outbox (status, created_at);
