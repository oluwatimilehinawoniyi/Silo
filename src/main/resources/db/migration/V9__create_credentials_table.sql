CREATE TABLE credentials (
    id                       UUID PRIMARY KEY,
    member_id                UUID         NOT NULL UNIQUE REFERENCES members (id),
    password_hash            VARCHAR(255) NOT NULL,
    role                     VARCHAR(20)  NOT NULL,
    refresh_token_hash       VARCHAR(255) UNIQUE,
    refresh_token_expires_at TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL
);
