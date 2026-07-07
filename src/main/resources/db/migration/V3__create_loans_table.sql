CREATE TABLE loans (
    id                  UUID PRIMARY KEY,
    loan_request_id     UUID           NOT NULL,
    member_id           UUID           NOT NULL REFERENCES members (id),
    principal_amount    NUMERIC(19, 2) NOT NULL,
    interest_rate       NUMERIC(5, 2)  NOT NULL,
    duration_months     INTEGER        NOT NULL,
    disbursed_date      TIMESTAMP,
    status              VARCHAR(20)    NOT NULL,
    outstanding_balance NUMERIC(19, 2) NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP      NOT NULL
);

CREATE UNIQUE INDEX idx_loans_loan_request_id ON loans (loan_request_id);
CREATE INDEX idx_loans_member_id ON loans (member_id);
CREATE INDEX idx_loans_status ON loans (status);
