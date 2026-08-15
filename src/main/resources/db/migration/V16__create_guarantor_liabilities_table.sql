CREATE TABLE guarantor_liabilities (
    id                  UUID PRIMARY KEY,
    loan_id             UUID           NOT NULL REFERENCES loans (id),
    guarantor_member_id UUID           NOT NULL REFERENCES members (id),
    amount              NUMERIC(19, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    assigned_at         TIMESTAMP      NOT NULL
);

CREATE INDEX idx_guarantor_liabilities_loan_id ON guarantor_liabilities (loan_id);
CREATE INDEX idx_guarantor_liabilities_guarantor_member_id ON guarantor_liabilities (guarantor_member_id);
