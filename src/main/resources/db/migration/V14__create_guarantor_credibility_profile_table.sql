CREATE TABLE guarantor_credibility_profile (
    member_id         UUID PRIMARY KEY REFERENCES members (id),
    times_guaranteed  INTEGER   NOT NULL DEFAULT 0,
    loans_went_bad    INTEGER   NOT NULL DEFAULT 0,
    credibility_score INTEGER   NOT NULL DEFAULT 100,
    updated_at        TIMESTAMP NOT NULL
);
