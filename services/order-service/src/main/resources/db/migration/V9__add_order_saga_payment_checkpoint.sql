ALTER TABLE order_sagas ADD COLUMN IF NOT EXISTS payment_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_sagas_payment_id
    ON order_sagas(payment_id)
    WHERE payment_id IS NOT NULL;
