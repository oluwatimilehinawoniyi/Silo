CREATE TABLE notification_log (
    id         UUID PRIMARY KEY,
    member_id  UUID        NOT NULL REFERENCES members (id),
    event_type VARCHAR(255) NOT NULL,
    channel    VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    sent_at    TIMESTAMP   NOT NULL
);

CREATE INDEX idx_notification_log_member_id ON notification_log (member_id);
