CREATE TABLE wishlists (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE wishlist_items (
    id UUID PRIMARY KEY, wishlist_id UUID NOT NULL REFERENCES wishlists(id), product_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (wishlist_id, product_id)
);

CREATE TABLE reviews (
    id UUID PRIMARY KEY, order_item_id UUID NOT NULL UNIQUE, order_id UUID NOT NULL, customer_id UUID NOT NULL,
    product_id UUID NOT NULL, variant_id UUID NOT NULL, rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT, status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE', 'HIDDEN')),
    hidden_reason VARCHAR(500), hidden_by UUID, hidden_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_product_visible_created ON reviews (product_id, created_at DESC) WHERE status = 'VISIBLE';
CREATE TABLE review_images (
    id UUID PRIMARY KEY, review_id UUID NOT NULL REFERENCES reviews(id), image_url VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), UNIQUE (review_id, sort_order)
);

CREATE TABLE product_questions (
    id UUID PRIMARY KEY, product_id UUID NOT NULL, customer_id UUID NOT NULL, content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ANSWERED', 'HIDDEN')),
    hidden_reason VARCHAR(500), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE product_answers (
    id UUID PRIMARY KEY, question_id UUID NOT NULL REFERENCES product_questions(id), admin_id UUID NOT NULL,
    content TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE notifications (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, type VARCHAR(50) NOT NULL, title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL, read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_customer_read_created ON notifications (customer_id, read_at, created_at DESC);

CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY, customer_id UUID NOT NULL, assigned_admin_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    last_message_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_conversations_customer ON chat_conversations (customer_id, status, last_message_at DESC);
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY, conversation_id UUID NOT NULL REFERENCES chat_conversations(id), sender_id UUID NOT NULL,
    sender_role VARCHAR(20) NOT NULL CHECK (sender_role IN ('CUSTOMER', 'ADMIN')), content TEXT NOT NULL,
    read_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);

CREATE TABLE daily_sales_metrics (
    metric_date DATE PRIMARY KEY, gross_item_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (gross_item_sales_vnd >= 0),
    discount_value_vnd BIGINT NOT NULL DEFAULT 0 CHECK (discount_value_vnd >= 0), shipping_fee_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_fee_vnd >= 0),
    net_revenue_vnd BIGINT NOT NULL DEFAULT 0 CHECK (net_revenue_vnd >= 0), order_count INTEGER NOT NULL DEFAULT 0 CHECK (order_count >= 0),
    completed_order_count INTEGER NOT NULL DEFAULT 0 CHECK (completed_order_count >= 0), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE daily_product_metrics (
    metric_date DATE NOT NULL, product_id UUID NOT NULL, variant_id UUID,
    quantity_sold INTEGER NOT NULL DEFAULT 0 CHECK (quantity_sold >= 0), gross_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (gross_sales_vnd >= 0),
    net_item_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (net_item_sales_vnd >= 0), review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0), rating_sum INTEGER NOT NULL DEFAULT 0 CHECK (rating_sum >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (metric_date, product_id, variant_id)
);

CREATE TABLE outbox_events (id UUID PRIMARY KEY, aggregate_type VARCHAR(80) NOT NULL, aggregate_id UUID NOT NULL, event_type VARCHAR(120) NOT NULL, event_version INTEGER NOT NULL, payload JSONB NOT NULL, correlation_id UUID, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0, available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_outbox_events_status_available ON outbox_events (status, available_at);
CREATE TABLE processed_events (event_id UUID PRIMARY KEY, event_type VARCHAR(120) NOT NULL, producer VARCHAR(80) NOT NULL, processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), correlation_id UUID);
CREATE TABLE idempotency_records (operation VARCHAR(100) NOT NULL, idempotency_key UUID NOT NULL, request_hash CHAR(64) NOT NULL, status_code INTEGER NOT NULL, response_body JSONB NOT NULL, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY (operation, idempotency_key));
