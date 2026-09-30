ALTER TABLE order_sagas ADD COLUMN payment_id UUID;

CREATE UNIQUE INDEX uq_order_sagas_payment_id
    ON order_sagas(payment_id)
    WHERE payment_id IS NOT NULL;
