CREATE TABLE promotion_audits (
    id UUID PRIMARY KEY,
    actor_admin_id UUID NOT NULL,
    target_type VARCHAR(30) NOT NULL CHECK (target_type IN ('DIRECT_SALE', 'VOUCHER', 'CUSTOMER_VOUCHER')),
    target_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (actor_admin_id, idempotency_key)
);
CREATE INDEX idx_promotion_audits_target ON promotion_audits (target_type, target_id, created_at DESC);
