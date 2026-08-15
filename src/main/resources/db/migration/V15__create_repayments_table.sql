CREATE TABLE repayments (
    id              UUID PRIMARY KEY,
    loan_id         UUID           NOT NULL REFERENCES loans (id),
    payer_member_id UUID           NOT NULL REFERENCES members (id),
    liability_id    UUID,
    amount          NUMERIC(19, 2) NOT NULL,
    reference       VARCHAR(255)   NOT NULL,
    payment_date    TIMESTAMP      NOT NULL
);

CREATE INDEX idx_repayments_loan_id ON repayments (loan_id);
