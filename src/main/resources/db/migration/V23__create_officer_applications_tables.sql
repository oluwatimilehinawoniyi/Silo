CREATE TABLE officer_applications (
    id          UUID PRIMARY KEY,
    member_id   UUID        NOT NULL REFERENCES members (id),
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    decided_at  TIMESTAMP
);

CREATE INDEX idx_officer_applications_member_id ON officer_applications (member_id);

CREATE TABLE officer_application_approvals (
    id             UUID      PRIMARY KEY,
    application_id UUID      NOT NULL REFERENCES officer_applications (id),
    officer_id     UUID      NOT NULL,
    approved_at    TIMESTAMP NOT NULL,
    UNIQUE (application_id, officer_id)
);

CREATE INDEX idx_officer_application_approvals_application_id ON officer_application_approvals (application_id);
