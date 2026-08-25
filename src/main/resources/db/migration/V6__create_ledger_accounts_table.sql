CREATE TABLE ledger_accounts (
    id         UUID PRIMARY KEY,
    code       VARCHAR(10)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX idx_ledger_accounts_code ON ledger_accounts (code);
