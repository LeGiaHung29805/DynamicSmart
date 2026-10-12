INSERT INTO daily_sales_metrics (
    metric_date, gross_item_sales_vnd, discount_value_vnd, shipping_fee_vnd,
    net_revenue_vnd, order_count, completed_order_count, updated_at)
SELECT
    CURRENT_DATE - i,
    1500000 + i * 175000,
    100000 + i * 10000,
    30000 + i * 5000,
    1430000 + i * 170000,
    8 + i,
    8 + i,
    NOW()
FROM generate_series(0, 6) AS s(i)
ON CONFLICT (metric_date) DO NOTHING;

-- Dữ liệu bán chạy local dùng cùng UUID với catalog seed. Một sản phẩm có thể
-- bán ở nhiều biến thể; API reporting sẽ cộng gộp chúng thành đúng một dòng.
WITH sample(product_no, variant_no, day_offset, quantity, unit_price) AS (
    VALUES
        (1, 1, 0, 12, 129000),
        (1, 2, 1,  8, 149000),
        (2, 1, 0, 17, 199000),
        (3, 1, 2, 15, 249000),
        (4, 1, 1, 13, 159000),
        (5, 1, 3, 11, 329000),
        (6, 1, 2,  9,  99000),
        (7, 1, 4,  8, 139000),
        (8, 1, 5,  7, 189000),
        (9, 1, 6,  6, 279000),
        (10, 1, 0, 5, 119000)
)
INSERT INTO daily_product_metrics (
    metric_date, product_id, variant_id, quantity_sold,
    gross_sales_vnd, net_item_sales_vnd, review_count, rating_sum, updated_at
)
SELECT
    CURRENT_DATE - day_offset,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((product_no - 1) * 3) + variant_no)::text, 12, '0'))::uuid,
    quantity,
    quantity::bigint * unit_price,
    quantity::bigint * unit_price,
    0,
    0,
    NOW()
FROM sample
ON CONFLICT (metric_date, product_id, variant_id) DO UPDATE SET
    quantity_sold = EXCLUDED.quantity_sold,
    gross_sales_vnd = EXCLUDED.gross_sales_vnd,
    net_item_sales_vnd = EXCLUDED.net_item_sales_vnd,
    updated_at = NOW();
