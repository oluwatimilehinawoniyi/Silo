CREATE TABLE reporting_loan_summary (
    loan_id             UUID PRIMARY KEY,
    member_id           UUID           NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    outstanding_balance NUMERIC(19, 2) NOT NULL,
    days_overdue        INTEGER        NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP      NOT NULL
);

CREATE INDEX idx_reporting_loan_summary_member_id ON reporting_loan_summary (member_id);
