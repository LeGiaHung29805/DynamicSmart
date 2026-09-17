ALTER TABLE payments
    ADD COLUMN cod_receipt_no VARCHAR(100),
    ADD COLUMN cod_confirmed_by UUID,
    ADD COLUMN cod_confirmed_at TIMESTAMPTZ;

CREATE TABLE shipping_quotes (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    province_id INTEGER NOT NULL REFERENCES ghn_location_provinces(id),
    ward_id INTEGER NOT NULL REFERENCES ghn_location_wards(id),
    items_fingerprint CHAR(64) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL UNIQUE,
    fee_vnd BIGINT NOT NULL CHECK (fee_vnd >= 0),
    shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0),
    service_id INTEGER NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    eta_text VARCHAR(150),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (shipping_discount_vnd <= fee_vnd)
);
CREATE INDEX idx_shipping_quotes_customer_expiry ON shipping_quotes (customer_id, expires_at);
