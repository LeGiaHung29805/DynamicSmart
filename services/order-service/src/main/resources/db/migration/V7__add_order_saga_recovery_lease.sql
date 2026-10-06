ALTER TABLE order_sagas
    DROP CONSTRAINT IF EXISTS order_sagas_status_check;

ALTER TABLE order_sagas
    ADD CONSTRAINT order_sagas_status_check CHECK (status IN (
        'STARTED', 'VOUCHER_RESERVED', 'INVENTORY_RESERVED', 'ORDER_CREATED',
        'PAYMENT_REQUESTED', 'FINALIZING_RESERVATIONS', 'INVENTORY_COMMITTED',
        'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED'
    ));

ALTER TABLE order_sagas
    ADD COLUMN recovery_owner VARCHAR(100),
    ADD COLUMN recovery_lease_until TIMESTAMPTZ;

CREATE INDEX idx_order_sagas_recovery_due
    ON order_sagas (recovery_lease_until, updated_at)
    WHERE status IN (
        'STARTED', 'VOUCHER_RESERVED', 'INVENTORY_RESERVED', 'ORDER_CREATED',
        'PAYMENT_REQUESTED', 'FINALIZING_RESERVATIONS', 'INVENTORY_COMMITTED', 'COMPENSATING'
    );
