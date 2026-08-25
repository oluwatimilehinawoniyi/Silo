CREATE TABLE ledger_entries (
    id             UUID PRIMARY KEY,
    transaction_id UUID           NOT NULL,
    account_id     UUID           NOT NULL REFERENCES ledger_accounts (id),
    entry_type     VARCHAR(10)    NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    description    VARCHAR(255),
    created_at     TIMESTAMP      NOT NULL
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id);
