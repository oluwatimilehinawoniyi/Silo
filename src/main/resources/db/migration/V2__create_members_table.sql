CREATE TABLE members (
    id              UUID PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(20)  NOT NULL,
    kyc_status      VARCHAR(20)  NOT NULL,
    id_type         VARCHAR(50),
    id_number       VARCHAR(100),
    id_document_ref VARCHAR(255),
    status          VARCHAR(20)  NOT NULL,
    joined_date     TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX idx_members_email ON members (email);
