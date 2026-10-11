CREATE TABLE daily_sales_metrics (
    metric_date DATE PRIMARY KEY,
    gross_item_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (gross_item_sales_vnd >= 0),
    discount_value_vnd BIGINT NOT NULL DEFAULT 0 CHECK (discount_value_vnd >= 0),
    shipping_fee_vnd BIGINT NOT NULL DEFAULT 0 CHECK (shipping_fee_vnd >= 0),
    net_revenue_vnd BIGINT NOT NULL DEFAULT 0 CHECK (net_revenue_vnd >= 0),
    order_count INTEGER NOT NULL DEFAULT 0 CHECK (order_count >= 0),
    completed_order_count INTEGER NOT NULL DEFAULT 0 CHECK (completed_order_count >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE daily_product_metrics (
    metric_date DATE NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID NOT NULL,
    quantity_sold INTEGER NOT NULL DEFAULT 0 CHECK (quantity_sold >= 0),
    gross_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (gross_sales_vnd >= 0),
    net_item_sales_vnd BIGINT NOT NULL DEFAULT 0 CHECK (net_item_sales_vnd >= 0),
    review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
    rating_sum INTEGER NOT NULL DEFAULT 0 CHECK (rating_sum >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (metric_date, product_id, variant_id)
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    producer VARCHAR(80) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    correlation_id UUID
);
