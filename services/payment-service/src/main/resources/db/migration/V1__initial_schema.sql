CREATE TABLE payments (
    id UUID PRIMARY KEY, order_id UUID NOT NULL UNIQUE, customer_id UUID NOT NULL,
    timing VARCHAR(20) NOT NULL CHECK (timing IN ('PREPAID', 'POSTPAID')), method VARCHAR(20) NOT NULL CHECK (method IN ('VNPAY', 'COD')),
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0), status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'EXPIRED')),
    expires_at TIMESTAMPTZ, paid_at TIMESTAMPTZ, failed_at TIMESTAMPTZ, provider_transaction_ref VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_status_expiry ON payments (status, expires_at);
CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY, payment_id UUID NOT NULL REFERENCES payments(id), attempt_no INTEGER NOT NULL,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('VNPAY', 'COD')), provider_reference VARCHAR(100) UNIQUE,
    amount_vnd BIGINT NOT NULL CHECK (amount_vnd > 0), request_payload JSONB, response_payload JSONB,
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'REDIRECTED', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    redirect_url TEXT, expires_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (payment_id, attempt_no)
);
CREATE TABLE payment_callback_audits (
    id UUID PRIMARY KEY, payment_id UUID REFERENCES payments(id), provider VARCHAR(30) NOT NULL,
    provider_transaction_ref VARCHAR(100), raw_payload JSONB NOT NULL, checksum_valid BOOLEAN NOT NULL,
    amount_valid BOOLEAN NOT NULL, processed_result VARCHAR(30) NOT NULL CHECK (processed_result IN ('APPLIED', 'DUPLICATE', 'REJECTED', 'LATE_AUDIT')),
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_callback_audits_reference ON payment_callback_audits (provider, provider_transaction_ref);

CREATE TABLE ghn_location_provinces (
    id INTEGER PRIMARY KEY, name VARCHAR(150) NOT NULL, name_normalized VARCHAR(150) NOT NULL,
    ghn_updated_at TIMESTAMPTZ, synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_ghn_provinces_active_name ON ghn_location_provinces (is_active, name_normalized);
CREATE TABLE ghn_location_wards (
    id INTEGER PRIMARY KEY, province_id INTEGER NOT NULL REFERENCES ghn_location_provinces(id),
    name VARCHAR(150) NOT NULL, name_normalized VARCHAR(150) NOT NULL, ghn_updated_at TIMESTAMPTZ,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_ghn_wards_province_active_name ON ghn_location_wards (province_id, is_active, name_normalized);

CREATE TABLE outbox_events (id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL, correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID);
CREATE TABLE idempotency_records (operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL, status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key));
