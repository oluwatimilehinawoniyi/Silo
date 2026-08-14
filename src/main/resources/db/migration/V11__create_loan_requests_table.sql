CREATE TABLE loan_requests (
    id                UUID PRIMARY KEY,
    member_id         UUID           NOT NULL REFERENCES members (id),
    amount_requested  NUMERIC(19, 2) NOT NULL,
    purpose           VARCHAR(255)   NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    submitted_at      TIMESTAMP      NOT NULL
);

CREATE INDEX idx_loan_requests_member_id ON loan_requests (member_id);
