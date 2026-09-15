CREATE TABLE checkout_sessions (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, source VARCHAR(20) NOT NULL CHECK (source IN ('CART', 'BUY_NOW')),
    cart_id UUID, selection_fingerprint CHAR(64) NOT NULL, address_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    payment_timing VARCHAR(20), payment_method VARCHAR(20),
    items_list_subtotal_vnd BIGINT CHECK (items_list_subtotal_vnd >= 0), direct_sale_discount_vnd BIGINT CHECK (direct_sale_discount_vnd >= 0),
    items_subtotal_vnd BIGINT CHECK (items_subtotal_vnd >= 0), product_discount_vnd BIGINT CHECK (product_discount_vnd >= 0), order_discount_vnd BIGINT CHECK (order_discount_vnd >= 0),
    shipping_fee_vnd BIGINT CHECK (shipping_fee_vnd >= 0), shipping_discount_vnd BIGINT CHECK (shipping_discount_vnd >= 0), final_total_vnd BIGINT CHECK (final_total_vnd >= 0),
    expires_at TIMESTAMPTZ NOT NULL, completed_order_id UUID, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK ((source = 'CART' AND cart_id IS NOT NULL) OR (source = 'BUY_NOW' AND cart_id IS NULL))
);
CREATE INDEX idx_checkout_sessions_customer_status_expiry ON checkout_sessions (customer_id, status, expires_at);
CREATE INDEX idx_checkout_sessions_status_expiry ON checkout_sessions (status, expires_at);

CREATE TABLE checkout_session_items (
    id UUID PRIMARY KEY, checkout_session_id UUID NOT NULL REFERENCES checkout_sessions(id), source_cart_item_id UUID, source_cart_item_version BIGINT,
    product_id UUID NOT NULL, variant_id UUID NOT NULL, sku VARCHAR(100) NOT NULL, product_name VARCHAR(300) NOT NULL, variant_name VARCHAR(300), image_url VARCHAR(1000),
    list_price_vnd BIGINT NOT NULL CHECK (list_price_vnd >= 0), direct_sale_promotion_id UUID, direct_sale_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (direct_sale_discount_vnd >= 0),
    unit_price_vnd BIGINT NOT NULL CHECK (unit_price_vnd >= 0), quantity INTEGER NOT NULL CHECK (quantity > 0), weight_grams INTEGER NOT NULL CHECK (weight_grams > 0), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (checkout_session_id, variant_id), CHECK (unit_price_vnd = list_price_vnd - direct_sale_discount_vnd)
);
CREATE TABLE checkout_session_vouchers (
    id UUID PRIMARY KEY, checkout_session_id UUID NOT NULL REFERENCES checkout_sessions(id), voucher_id UUID NOT NULL, voucher_reservation_id UUID,
    voucher_code VARCHAR(80) NOT NULL, scope VARCHAR(30) NOT NULL, discount_amount_vnd BIGINT NOT NULL CHECK (discount_amount_vnd >= 0),
    shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (checkout_session_id, voucher_id)
);
CREATE UNIQUE INDEX uq_checkout_one_shipping_voucher ON checkout_session_vouchers (checkout_session_id) WHERE scope = 'SHIPPING_DISCOUNT';
CREATE UNIQUE INDEX uq_checkout_one_merchandise_voucher ON checkout_session_vouchers (checkout_session_id) WHERE scope <> 'SHIPPING_DISCOUNT';
CREATE TABLE checkout_shipping_quotes (
    id UUID PRIMARY KEY, checkout_session_id UUID NOT NULL REFERENCES checkout_sessions(id), quote_id UUID NOT NULL, input_fingerprint CHAR(64) NOT NULL,
    fee_vnd BIGINT NOT NULL CHECK (fee_vnd >= 0), service_id INTEGER NOT NULL, service_name VARCHAR(200), eta TIMESTAMPTZ, expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'INVALID')), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_checkout_shipping_quotes_session_status ON checkout_shipping_quotes (checkout_session_id, status, expires_at);

CREATE TABLE orders (
    id UUID PRIMARY KEY, order_number VARCHAR(32) NOT NULL UNIQUE, checkout_session_id UUID NOT NULL UNIQUE, customer_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'PACKING', 'SHIPPING', 'HANDOVER_PENDING', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    payment_timing VARCHAR(20) NOT NULL CHECK (payment_timing IN ('PREPAID', 'POSTPAID', 'NOT_REQUIRED')),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('VNPAY', 'COD', 'FREE')),
    items_list_subtotal_vnd BIGINT NOT NULL CHECK (items_list_subtotal_vnd >= 0), direct_sale_discount_vnd BIGINT NOT NULL CHECK (direct_sale_discount_vnd >= 0),
    items_subtotal_vnd BIGINT NOT NULL CHECK (items_subtotal_vnd >= 0), product_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (product_discount_vnd >= 0), order_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (order_discount_vnd >= 0),
    shipping_fee_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_fee_vnd >= 0), shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0), final_total_vnd BIGINT NOT NULL CHECK (final_total_vnd >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND', payment_due_at TIMESTAMPTZ, payment_succeeded_at TIMESTAMPTZ, shipment_delivered_at TIMESTAMPTZ,
    cancel_reason VARCHAR(80), cancelled_at TIMESTAMPTZ, confirmed_at TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK ((payment_timing = 'PREPAID' AND payment_method = 'VNPAY') OR (payment_timing = 'POSTPAID' AND payment_method IN ('VNPAY', 'COD')) OR (payment_timing = 'NOT_REQUIRED' AND payment_method = 'FREE')),
    CHECK (items_subtotal_vnd = items_list_subtotal_vnd - direct_sale_discount_vnd),
    CHECK (final_total_vnd = items_subtotal_vnd - product_discount_vnd - order_discount_vnd + shipping_fee_vnd - shipping_discount_vnd)
);
CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_status_created ON orders (status, created_at DESC);
CREATE TABLE order_addresses (
    id UUID PRIMARY KEY, order_id UUID NOT NULL UNIQUE REFERENCES orders(id), source_address_id UUID,
    recipient_name VARCHAR(150) NOT NULL, phone VARCHAR(20) NOT NULL, address_line VARCHAR(500) NOT NULL,
    province_id INTEGER NOT NULL, ward_id INTEGER NOT NULL, province_name VARCHAR(150) NOT NULL, ward_name VARCHAR(150) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE order_items (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES orders(id), product_id UUID NOT NULL, variant_id UUID NOT NULL, sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(300) NOT NULL, variant_name VARCHAR(300), image_url VARCHAR(1000), list_price_vnd BIGINT NOT NULL CHECK (list_price_vnd >= 0),
    direct_sale_promotion_id UUID, direct_sale_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (direct_sale_discount_vnd >= 0),
    unit_price_vnd BIGINT NOT NULL CHECK (unit_price_vnd >= 0), quantity INTEGER NOT NULL CHECK (quantity > 0),
    product_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (product_discount_vnd >= 0), order_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (order_discount_vnd >= 0),
    line_total_vnd BIGINT NOT NULL CHECK (line_total_vnd >= 0), weight_grams INTEGER NOT NULL CHECK (weight_grams > 0), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (unit_price_vnd = list_price_vnd - direct_sale_discount_vnd)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
CREATE TABLE order_voucher_snapshots (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES orders(id), voucher_id UUID, voucher_code VARCHAR(80) NOT NULL,
    scope VARCHAR(30) NOT NULL, discount_method VARCHAR(20) NOT NULL, discount_value BIGINT, eligible_subtotal_vnd BIGINT NOT NULL CHECK (eligible_subtotal_vnd >= 0),
    discount_amount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (discount_amount_vnd >= 0), shipping_discount_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_discount_vnd >= 0), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE order_shipping_snapshots (
    id UUID PRIMARY KEY, order_id UUID NOT NULL UNIQUE REFERENCES orders(id), quote_id UUID NOT NULL, input_fingerprint CHAR(64) NOT NULL,
    service_id INTEGER NOT NULL, service_name VARCHAR(200), fee_vnd BIGINT NOT NULL CHECK (fee_vnd >= 0), eta TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY, order_id UUID NOT NULL REFERENCES orders(id), from_status VARCHAR(40), to_status VARCHAR(40) NOT NULL,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('SYSTEM', 'CUSTOMER', 'ADMIN')), actor_id UUID, reason VARCHAR(500), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_status_history_order_created ON order_status_history (order_id, created_at);
CREATE TABLE order_operation_log (operation_key UUID PRIMARY KEY, operation_type VARCHAR(50) NOT NULL, order_id UUID, result_payload JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());

CREATE TABLE outbox_events (id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL, correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID);
CREATE TABLE idempotency_records (operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL, status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key));
