ALTER TABLE checkout_sessions
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE checkout_shipping_quotes
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'GHN',
    ADD COLUMN shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0),
    ADD COLUMN payable_fee_vnd BIGINT NOT NULL DEFAULT 0 CHECK (payable_fee_vnd >= 0),
    ADD COLUMN eta_text VARCHAR(200),
    ADD COLUMN total_weight_grams INTEGER CHECK (total_weight_grams > 0),
    ADD COLUMN package_length_cm INTEGER CHECK (package_length_cm > 0),
    ADD COLUMN package_width_cm INTEGER CHECK (package_width_cm > 0),
    ADD COLUMN package_height_cm INTEGER CHECK (package_height_cm > 0),
    ADD COLUMN to_province_id INTEGER,
    ADD COLUMN to_ward_id INTEGER,
    ADD COLUMN to_province_name VARCHAR(150),
    ADD COLUMN to_ward_name VARCHAR(150),
    ADD COLUMN quoted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN consumed_at TIMESTAMPTZ,
    ADD COLUMN raw_response_redacted JSONB;

ALTER TABLE checkout_shipping_quotes
    DROP CONSTRAINT checkout_shipping_quotes_status_check;

UPDATE checkout_shipping_quotes
SET payable_fee_vnd = GREATEST(fee_vnd - shipping_discount_vnd, 0),
    status = CASE
        WHEN status = 'USED' THEN 'CONSUMED'
        WHEN status = 'INVALID' THEN 'INVALIDATED'
        ELSE status
    END;

ALTER TABLE checkout_shipping_quotes
    ADD CONSTRAINT checkout_shipping_quotes_status_check
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'INVALIDATED', 'EXPIRED')),
    ADD CONSTRAINT checkout_shipping_quotes_payable_fee_check
        CHECK (payable_fee_vnd = fee_vnd - shipping_discount_vnd),
    ADD CONSTRAINT checkout_shipping_quotes_discount_check
        CHECK (shipping_discount_vnd <= fee_vnd);

CREATE UNIQUE INDEX uq_checkout_shipping_quotes_provider_quote
    ON checkout_shipping_quotes (provider, quote_id);

CREATE INDEX idx_checkout_shipping_quotes_quote_status
    ON checkout_shipping_quotes (quote_id, status, expires_at);

ALTER TABLE order_shipping_snapshots
    ADD COLUMN provider VARCHAR(30) NOT NULL DEFAULT 'GHN',
    ADD COLUMN source_checkout_quote_id UUID REFERENCES checkout_shipping_quotes(id),
    ADD COLUMN shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0),
    ADD COLUMN payable_fee_vnd BIGINT NOT NULL DEFAULT 0 CHECK (payable_fee_vnd >= 0),
    ADD COLUMN eta_text VARCHAR(200),
    ADD COLUMN total_weight_grams INTEGER CHECK (total_weight_grams > 0),
    ADD COLUMN package_length_cm INTEGER CHECK (package_length_cm > 0),
    ADD COLUMN package_width_cm INTEGER CHECK (package_width_cm > 0),
    ADD COLUMN package_height_cm INTEGER CHECK (package_height_cm > 0),
    ADD COLUMN to_province_id INTEGER,
    ADD COLUMN to_ward_id INTEGER,
    ADD COLUMN to_province_name VARCHAR(150),
    ADD COLUMN to_ward_name VARCHAR(150),
    ADD COLUMN quoted_at TIMESTAMPTZ,
    ADD COLUMN raw_response_redacted JSONB;

UPDATE order_shipping_snapshots
SET payable_fee_vnd = GREATEST(fee_vnd - shipping_discount_vnd, 0);

ALTER TABLE order_shipping_snapshots
    ADD CONSTRAINT order_shipping_snapshots_payable_fee_check
        CHECK (payable_fee_vnd = fee_vnd - shipping_discount_vnd),
    ADD CONSTRAINT order_shipping_snapshots_discount_check
        CHECK (shipping_discount_vnd <= fee_vnd);

ALTER TABLE order_status_history
    ADD COLUMN correlation_id UUID,
    ADD COLUMN source_event_id UUID;

ALTER TABLE order_status_history
    DROP CONSTRAINT order_status_history_actor_type_check,
    ADD CONSTRAINT order_status_history_actor_type_check
        CHECK (actor_type IN ('SYSTEM', 'CUSTOMER', 'ADMIN', 'PAYMENT_EVENT'));

CREATE UNIQUE INDEX uq_order_status_history_source_event
    ON order_status_history (source_event_id)
    WHERE source_event_id IS NOT NULL;

CREATE TABLE order_sagas (
    id UUID PRIMARY KEY,
    checkout_session_id UUID NOT NULL UNIQUE REFERENCES checkout_sessions(id),
    order_id UUID UNIQUE REFERENCES orders(id),
    status VARCHAR(40) NOT NULL CHECK (status IN (
        'STARTED', 'VOUCHER_RESERVED', 'INVENTORY_RESERVED', 'ORDER_CREATED',
        'PAYMENT_REQUESTED', 'COMPLETED', 'COMPENSATING', 'COMPENSATED', 'FAILED'
    )),
    current_step VARCHAR(80) NOT NULL,
    voucher_reservation_id UUID,
    inventory_reservation_id UUID,
    correlation_id UUID NOT NULL UNIQUE,
    last_error_code VARCHAR(100),
    last_error_message VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_order_sagas_status_updated
    ON order_sagas (status, updated_at);

CREATE INDEX idx_order_sagas_order
    ON order_sagas (order_id)
    WHERE order_id IS NOT NULL;
