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
