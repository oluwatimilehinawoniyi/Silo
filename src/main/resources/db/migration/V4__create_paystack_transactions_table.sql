CREATE TABLE paystack_transactions (
    id UUID PRIMARY KEY,
    paystack_reference VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    member_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT idx_paystack_transactions_reference UNIQUE (paystack_reference),
    CONSTRAINT fk_paystack_transactions_member FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE INDEX idx_paystack_transactions_member_id ON paystack_transactions (member_id);
CREATE INDEX idx_paystack_transactions_status ON paystack_transactions (status);