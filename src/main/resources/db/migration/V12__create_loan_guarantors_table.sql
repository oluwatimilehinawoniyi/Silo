CREATE TABLE loan_guarantors (
    id              UUID PRIMARY KEY,
    loan_request_id UUID        NOT NULL REFERENCES loan_requests (id),
    member_id       UUID        NOT NULL REFERENCES members (id),
    status          VARCHAR(20) NOT NULL,
    invited_at      TIMESTAMP   NOT NULL,
    UNIQUE (loan_request_id, member_id)
);

CREATE INDEX idx_loan_guarantors_loan_request_id ON loan_guarantors (loan_request_id);
CREATE INDEX idx_loan_guarantors_member_id ON loan_guarantors (member_id);
