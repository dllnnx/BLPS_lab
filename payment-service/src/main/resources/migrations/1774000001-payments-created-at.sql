ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now();

COMMENT ON COLUMN payments.created_at IS 'время создания счёта; для истечения PENDING';
