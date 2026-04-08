CREATE TABLE monthly_summaries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    total_debit     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_credit    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    top_category    VARCHAR(100),
    ai_insights     TEXT,
    generated_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE (user_id, year, month)
);

CREATE INDEX idx_monthly_summaries_user ON monthly_summaries(user_id, year DESC, month DESC);