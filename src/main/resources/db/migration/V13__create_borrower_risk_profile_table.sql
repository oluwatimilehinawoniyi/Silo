CREATE TABLE borrower_risk_profile (
    member_id          UUID PRIMARY KEY REFERENCES members (id),
    total_loans        INTEGER     NOT NULL DEFAULT 0,
    defaulted_loans    INTEGER     NOT NULL DEFAULT 0,
    late_loan_payments INTEGER     NOT NULL DEFAULT 0,
    current_risk_tier  VARCHAR(20) NOT NULL,
    updated_at         TIMESTAMP   NOT NULL
);
