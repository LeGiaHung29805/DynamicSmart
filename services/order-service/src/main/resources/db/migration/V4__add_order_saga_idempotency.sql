ALTER TABLE order_sagas
    ADD COLUMN idempotency_key UUID,
    ADD COLUMN request_hash CHAR(64);

UPDATE order_sagas
SET idempotency_key = id,
    request_hash = repeat('0', 64)
WHERE idempotency_key IS NULL OR request_hash IS NULL;

ALTER TABLE order_sagas
    ALTER COLUMN idempotency_key SET NOT NULL,
    ALTER COLUMN request_hash SET NOT NULL;

CREATE UNIQUE INDEX uq_order_sagas_idempotency_key
    ON order_sagas (idempotency_key);
