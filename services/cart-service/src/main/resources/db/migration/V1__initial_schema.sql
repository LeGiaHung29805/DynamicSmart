CREATE TABLE carts (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'MERGED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE cart_items (
    id UUID PRIMARY KEY, cart_id UUID NOT NULL REFERENCES carts(id), product_id UUID NOT NULL, variant_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0), version BIGINT NOT NULL DEFAULT 0, is_selected BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (cart_id, variant_id)
);
CREATE INDEX idx_cart_items_cart_selected ON cart_items (cart_id, is_selected);

CREATE TABLE direct_price_promotions (
    id UUID PRIMARY KEY, name VARCHAR(200) NOT NULL, description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'EXPIRED', 'ARCHIVED')),
    discount_method VARCHAR(20) NOT NULL CHECK (discount_method IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    fixed_discount_vnd BIGINT CHECK (fixed_discount_vnd >= 0), discount_rate_bps INTEGER CHECK (discount_rate_bps > 0),
    max_discount_vnd BIGINT CHECK (max_discount_vnd >= 0), starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at),
    CHECK ((discount_method = 'FIXED_AMOUNT' AND fixed_discount_vnd IS NOT NULL AND discount_rate_bps IS NULL) OR (discount_method = 'PERCENTAGE' AND discount_rate_bps IS NOT NULL AND fixed_discount_vnd IS NULL))
);
CREATE INDEX idx_direct_price_promotions_active_window ON direct_price_promotions (status, starts_at, ends_at);
CREATE TABLE direct_price_promotion_variants (
    promotion_id UUID NOT NULL REFERENCES direct_price_promotions(id), variant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (promotion_id, variant_id)
);
CREATE INDEX idx_direct_price_promotion_variants_variant ON direct_price_promotion_variants (variant_id);

CREATE TABLE vouchers (
    id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL, description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED', 'EXPIRED')),
    scope VARCHAR(30) NOT NULL CHECK (scope IN ('ORDER_DISCOUNT', 'SHIPPING_DISCOUNT', 'PRODUCT_DISCOUNT', 'CATEGORY_DISCOUNT', 'PRODUCT_LIST_DISCOUNT')),
    discount_method VARCHAR(20) NOT NULL CHECK (discount_method IN ('FIXED_AMOUNT', 'PERCENTAGE')),
    fixed_discount_vnd BIGINT CHECK (fixed_discount_vnd >= 0), discount_rate_bps INTEGER CHECK (discount_rate_bps > 0), max_discount_vnd BIGINT CHECK (max_discount_vnd >= 0),
    minimum_order_vnd BIGINT CHECK (minimum_order_vnd >= 0), minimum_eligible_subtotal_vnd BIGINT CHECK (minimum_eligible_subtotal_vnd >= 0),
    usage_limit INTEGER CHECK (usage_limit > 0), consumed_count INTEGER NOT NULL DEFAULT 0 CHECK (consumed_count >= 0), usage_limit_per_customer INTEGER CHECK (usage_limit_per_customer > 0),
    starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
    distribution_mode VARCHAR(30) NOT NULL CHECK (distribution_mode IN ('DEFAULT_FOR_ELIGIBLE', 'ASSIGNED_ONLY', 'CODE_ONLY')),
    is_default BOOLEAN NOT NULL DEFAULT FALSE, created_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at), CHECK (consumed_count <= COALESCE(usage_limit, consumed_count)),
    CHECK ((discount_method = 'FIXED_AMOUNT' AND fixed_discount_vnd IS NOT NULL AND discount_rate_bps IS NULL) OR (discount_method = 'PERCENTAGE' AND discount_rate_bps IS NOT NULL AND fixed_discount_vnd IS NULL)),
    CHECK (NOT is_default OR distribution_mode = 'DEFAULT_FOR_ELIGIBLE')
);
CREATE INDEX idx_vouchers_active_window ON vouchers (status, starts_at, ends_at);
CREATE TABLE voucher_products (voucher_id UUID NOT NULL REFERENCES vouchers(id), product_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (voucher_id, product_id));
CREATE TABLE voucher_categories (voucher_id UUID NOT NULL REFERENCES vouchers(id), category_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (voucher_id, category_id));
CREATE TABLE customer_vouchers (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, voucher_id UUID NOT NULL REFERENCES vouchers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'USED_UP', 'REVOKED', 'EXPIRED')),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), expires_at TIMESTAMPTZ, source VARCHAR(30) NOT NULL CHECK (source IN ('ADMIN_ASSIGNMENT', 'CAMPAIGN', 'WALLET')),
    assigned_by UUID, UNIQUE (customer_id, voucher_id), UNIQUE (id, customer_id, voucher_id)
);
CREATE INDEX idx_customer_vouchers_customer_status ON customer_vouchers (customer_id, status);
CREATE TABLE voucher_reservations (
    id UUID PRIMARY KEY, voucher_id UUID NOT NULL REFERENCES vouchers(id), customer_id UUID NOT NULL, customer_voucher_id UUID,
    checkout_session_id UUID NOT NULL, order_id UUID, status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'EXPIRED')),
    discount_amount_vnd BIGINT NOT NULL CHECK (discount_amount_vnd >= 0), shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0),
    reserved_until TIMESTAMPTZ NOT NULL, consumed_at TIMESTAMPTZ, released_at TIMESTAMPTZ, release_reason VARCHAR(80), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (checkout_session_id, voucher_id),
    FOREIGN KEY (customer_voucher_id, customer_id, voucher_id) REFERENCES customer_vouchers (id, customer_id, voucher_id)
);
CREATE INDEX idx_voucher_reservations_status_expiry ON voucher_reservations (status, reserved_until);

CREATE TABLE outbox_events (id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL, correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID);
CREATE TABLE idempotency_records (operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL, status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key));
