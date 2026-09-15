CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    auth_version INTEGER NOT NULL DEFAULT 0 CHECK (auth_version >= 0),
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role_status ON users (role, status);
CREATE INDEX idx_users_phone ON users (phone) WHERE phone IS NOT NULL;

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_tokens(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user_expiry ON refresh_tokens (user_id, expires_at);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    requested_ip_hash CHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_password_reset_tokens_user_expiry ON password_reset_tokens (user_id, expires_at);

CREATE TABLE user_management_audits (
    id UUID PRIMARY KEY,
    target_user_id UUID NOT NULL REFERENCES users(id),
    actor_admin_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(30) NOT NULL CHECK (action IN ('LOCK', 'UNLOCK', 'DISABLE', 'CHANGE_ROLE')),
    old_role VARCHAR(20), new_role VARCHAR(20),
    old_status VARCHAR(20), new_status VARCHAR(20),
    reason VARCHAR(500) NOT NULL,
    idempotency_key UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (actor_admin_id, idempotency_key)
);
CREATE INDEX idx_user_management_audits_target ON user_management_audits (target_user_id, created_at DESC);

CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    province_id INTEGER NOT NULL,
    ward_id INTEGER NOT NULL,
    province_name VARCHAR(150) NOT NULL,
    ward_name VARCHAR(150) NOT NULL,
    location_validated_at TIMESTAMPTZ NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    deactivated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_addresses_user_status ON addresses (user_id, status);
CREATE UNIQUE INDEX uq_addresses_one_active_default ON addresses (user_id) WHERE is_default AND status = 'ACTIVE';

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL,
    correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID
);

CREATE TABLE idempotency_records (
    operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL,
    status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key)
);
