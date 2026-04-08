CREATE TABLE transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id      INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    description      VARCHAR(500) NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL,
    type             VARCHAR(10) NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    transaction_date DATE NOT NULL,
    source           VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_id        ON transactions(user_id);
CREATE INDEX idx_transactions_date           ON transactions(transaction_date);
CREATE INDEX idx_transactions_user_date      ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_category  ON transactions(user_id, category_id);