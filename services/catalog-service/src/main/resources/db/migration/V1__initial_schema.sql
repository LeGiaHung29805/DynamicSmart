CREATE TABLE categories (
    id UUID PRIMARY KEY, parent_id UUID REFERENCES categories(id), code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL, slug VARCHAR(220) NOT NULL UNIQUE, description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    sort_order INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX idx_categories_parent_status ON categories (parent_id, status);

CREATE TABLE attributes (
    id UUID PRIMARY KEY, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
    data_type VARCHAR(20) NOT NULL CHECK (data_type IN ('TEXT', 'NUMBER', 'DECIMAL', 'BOOLEAN', 'SELECT', 'MULTI_SELECT')),
    validation_config JSONB, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE attribute_options (
    id UUID PRIMARY KEY, attribute_id UUID NOT NULL REFERENCES attributes(id), code VARCHAR(80) NOT NULL,
    label VARCHAR(120) NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (attribute_id, code)
);
CREATE TABLE category_attributes (
    id UUID PRIMARY KEY, category_id UUID NOT NULL REFERENCES categories(id), attribute_id UUID NOT NULL REFERENCES attributes(id),
    applies_to VARCHAR(20) NOT NULL CHECK (applies_to IN ('PRODUCT', 'VARIANT')), is_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_filterable BOOLEAN NOT NULL DEFAULT FALSE, sort_order INTEGER NOT NULL DEFAULT 0, UNIQUE (category_id, attribute_id, applies_to)
);

CREATE TABLE products (
    id UUID PRIMARY KEY, category_id UUID NOT NULL REFERENCES categories(id), name VARCHAR(300) NOT NULL,
    slug VARCHAR(330) NOT NULL UNIQUE, short_description VARCHAR(1000), description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    default_weight_grams INTEGER CHECK (default_weight_grams > 0),
    default_length_cm INTEGER CHECK (default_length_cm > 0), default_width_cm INTEGER CHECK (default_width_cm > 0), default_height_cm INTEGER CHECK (default_height_cm > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_category_status ON products (category_id, status);

CREATE TABLE product_variants (
    id UUID PRIMARY KEY, product_id UUID NOT NULL REFERENCES products(id), sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(300), price_vnd BIGINT NOT NULL CHECK (price_vnd >= 0), weight_grams INTEGER NOT NULL CHECK (weight_grams > 0),
    length_cm INTEGER CHECK (length_cm > 0), width_cm INTEGER CHECK (width_cm > 0), height_cm INTEGER CHECK (height_cm > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    sort_order INTEGER NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (id, product_id)
);
CREATE INDEX idx_product_variants_product_status ON product_variants (product_id, status);

CREATE TABLE product_images (
    id UUID PRIMARY KEY, product_id UUID NOT NULL REFERENCES products(id), variant_id UUID,
    image_url VARCHAR(1000) NOT NULL, alt_text VARCHAR(300), sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    FOREIGN KEY (variant_id, product_id) REFERENCES product_variants(id, product_id)
);
CREATE UNIQUE INDEX uq_product_primary_image ON product_images (product_id) WHERE is_primary AND variant_id IS NULL;
CREATE UNIQUE INDEX uq_variant_primary_image ON product_images (variant_id) WHERE is_primary AND variant_id IS NOT NULL;

CREATE TABLE product_attribute_values (
    id UUID PRIMARY KEY, product_id UUID NOT NULL REFERENCES products(id), attribute_id UUID NOT NULL REFERENCES attributes(id),
    value_json JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (product_id, attribute_id)
);
CREATE TABLE variant_attribute_values (
    id UUID PRIMARY KEY, variant_id UUID NOT NULL REFERENCES product_variants(id), attribute_id UUID NOT NULL REFERENCES attributes(id),
    value_json JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (variant_id, attribute_id)
);

CREATE TABLE inventory_items (
    variant_id UUID PRIMARY KEY REFERENCES product_variants(id), on_hand_qty INTEGER NOT NULL CHECK (on_hand_qty >= 0),
    reserved_qty INTEGER NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0), version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), CHECK (reserved_qty <= on_hand_qty)
);
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY, checkout_session_id UUID NOT NULL UNIQUE, order_id UUID,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'COMMITTED', 'RELEASED', 'EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL, committed_at TIMESTAMPTZ, released_at TIMESTAMPTZ, release_reason VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inventory_reservations_status_expiry ON inventory_reservations (status, expires_at);
CREATE TABLE inventory_reservation_items (
    id UUID PRIMARY KEY, reservation_id UUID NOT NULL REFERENCES inventory_reservations(id), variant_id UUID NOT NULL REFERENCES product_variants(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0), UNIQUE (reservation_id, variant_id)
);
CREATE TABLE inventory_operation_log (
    operation_key UUID PRIMARY KEY, operation_type VARCHAR(30) NOT NULL CHECK (operation_type IN ('RESERVE', 'COMMIT', 'RELEASE')),
    reservation_id UUID, result_payload JSONB NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE inventory_adjustments (
    id UUID PRIMARY KEY, operation_key UUID NOT NULL UNIQUE, variant_id UUID NOT NULL REFERENCES product_variants(id),
    quantity_delta INTEGER NOT NULL CHECK (quantity_delta <> 0), on_hand_before INTEGER NOT NULL CHECK (on_hand_before >= 0),
    on_hand_after INTEGER NOT NULL CHECK (on_hand_after >= 0), reason VARCHAR(500) NOT NULL, actor_admin_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL, correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID);
CREATE TABLE idempotency_records (operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL, status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key));
