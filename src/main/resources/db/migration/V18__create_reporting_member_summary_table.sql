CREATE TABLE reporting_member_summary (
    member_id           UUID PRIMARY KEY,
    total_contributions NUMERIC(19, 2) NOT NULL DEFAULT 0,
    active_loans        INTEGER        NOT NULL DEFAULT 0,
    total_repayments    NUMERIC(19, 2) NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP      NOT NULL
);
