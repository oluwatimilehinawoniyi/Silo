ALTER TABLE paystack_transactions
    ADD COLUMN authorization_code VARCHAR(120);

CREATE TABLE contribution_auto_debit_mandates (
    id                         UUID PRIMARY KEY,
    member_id                  UUID           NOT NULL REFERENCES members (id),
    paystack_authorization_code VARCHAR(120)  NOT NULL,
    amount                     NUMERIC(19, 2) NOT NULL,
    periodicity                VARCHAR(20)    NOT NULL,
    status                     VARCHAR(20)    NOT NULL,
    next_charge_date           DATE           NOT NULL,
    consecutive_failure_count  INTEGER        NOT NULL DEFAULT 0,
    last_failure_reason        VARCHAR(500),
    created_at                 TIMESTAMP      NOT NULL
);

CREATE INDEX idx_auto_debit_mandate_member_id ON contribution_auto_debit_mandates (member_id);
CREATE INDEX idx_auto_debit_mandate_status_next_charge ON contribution_auto_debit_mandates (status, next_charge_date);

-- Only one non-cancelled mandate per member at a time; "replacing" a mandate
-- cancels the old one first rather than allowing two to coexist.
CREATE UNIQUE INDEX idx_auto_debit_mandate_member_active
    ON contribution_auto_debit_mandates (member_id)
    WHERE status IN ('ACTIVE', 'PAUSED', 'FAILED');
