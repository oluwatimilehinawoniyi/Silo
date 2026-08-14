CREATE TABLE contributions
(
    id                UUID PRIMARY KEY,
    member_id         UUID           NOT NULL REFERENCES members (id),
    amount            NUMERIC(19, 2) NOT NULL,
    reference         VARCHAR(255)   NOT NULL,
    source            VARCHAR(20)    NOT NULL,
    recorded_by       UUID,
    contribution_date TIMESTAMP      NOT NULL
);

CREATE INDEX idx_contributions_member_id ON contributions (member_id);
