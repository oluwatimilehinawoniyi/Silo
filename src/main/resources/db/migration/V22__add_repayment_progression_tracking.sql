ALTER TABLE loan_installments
    ADD COLUMN amount_paid NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE guarantor_liabilities
    ADD COLUMN amount_paid NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE guarantor_credibility_profile
    ADD COLUMN successful_guarantees INTEGER NOT NULL DEFAULT 0;
