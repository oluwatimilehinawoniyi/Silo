CREATE TABLE loan_installments (
    id                 UUID PRIMARY KEY,
    loan_id            UUID           NOT NULL REFERENCES loans (id),
    installment_number INTEGER        NOT NULL,
    due_date           TIMESTAMP      NOT NULL,
    expected_amount    NUMERIC(19, 2) NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    paid_date          TIMESTAMP,
    version            BIGINT         NOT NULL DEFAULT 0,
    created_at         TIMESTAMP      NOT NULL
);

CREATE UNIQUE INDEX idx_loan_installments_loan_id_number ON loan_installments (loan_id, installment_number);
CREATE INDEX idx_loan_installments_status ON loan_installments (status);
CREATE INDEX idx_loan_installments_due_date ON loan_installments (due_date);
