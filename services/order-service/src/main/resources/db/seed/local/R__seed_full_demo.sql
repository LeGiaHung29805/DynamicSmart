-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           ((i - 1) % 12) + 1 AS customer_no,
           CASE (i % 8)
               WHEN 1 THEN 'PENDING_PAYMENT'
               WHEN 2 THEN 'CONFIRMED'
               WHEN 3 THEN 'PACKING'
               WHEN 4 THEN 'SHIPPING'
               WHEN 5 THEN 'HANDOVER_PENDING'
               WHEN 6 THEN 'DELIVERED'
               WHEN 7 THEN 'COMPLETED'
               ELSE 'CANCELLED'
           END AS order_status,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
), priced AS (
    SELECT *, 79000 + product_no * 7000 AS unit_price
    FROM data
)
INSERT INTO checkout_sessions (
    id, customer_id, source, selection_fingerprint, address_id, status,
    payment_timing, payment_method, items_list_subtotal_vnd,
    direct_sale_discount_vnd, items_subtotal_vnd, product_discount_vnd,
    order_discount_vnd, shipping_fee_vnd, shipping_discount_vnd,
    final_total_vnd, expires_at, completed_order_id, created_at, updated_at
)
SELECT
    ('d1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    'BUY_NOW', repeat(substr(md5('seed-session-' || i), 1, 32), 2),
    ('a2000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    'COMPLETED',
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'PREPAID' ELSE 'POSTPAID' END,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'VNPAY' ELSE 'COD' END,
    unit_price * quantity, 0, unit_price * quantity, 0, 0, 30000, 0,
    unit_price * quantity + 30000,
    NOW() + INTERVAL '30 minutes',
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 49 - i)
FROM priced
ON CONFLICT (id) DO UPDATE SET status = 'COMPLETED', completed_order_id = EXCLUDED.completed_order_id, updated_at = NOW();

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO checkout_session_items (
    id, checkout_session_id, product_id, variant_id, sku, product_name, variant_name,
    image_url, list_price_vnd, direct_sale_discount_vnd, unit_price_vnd,
    quantity, weight_grams, length_cm, width_cm, height_cm, created_at
)
SELECT
    ('d2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((product_no - 1) * 3) + 1)::text, 12, '0'))::uuid,
    'DM-' || lpad(product_no::text, 3, '0') || '-1', 'Sản phẩm DynamicMart ' || lpad(product_no::text, 3, '0'), 'Tiêu chuẩn',
    CASE ((product_no - 1) % 6) + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1000&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1000&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1000&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=80'
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1000&q=80'
    END,
    79000 + product_no * 7000, 0, 79000 + product_no * 7000,
    quantity, 250 + (product_no % 8) * 100,
    20 + (product_no % 5), 15 + (product_no % 4), 5 + (product_no % 3),
    NOW() - make_interval(days => 49 - i)
FROM data
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           ((i - 1) % 12) + 1 AS customer_no,
           CASE (i % 8)
               WHEN 1 THEN 'PENDING_PAYMENT' WHEN 2 THEN 'CONFIRMED'
               WHEN 3 THEN 'PACKING' WHEN 4 THEN 'SHIPPING'
               WHEN 5 THEN 'HANDOVER_PENDING' WHEN 6 THEN 'DELIVERED'
               WHEN 7 THEN 'COMPLETED' ELSE 'CANCELLED'
           END AS order_status,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
), priced AS (
    SELECT *, 79000 + product_no * 7000 AS unit_price
    FROM data
)
INSERT INTO orders (
    id, order_number, checkout_session_id, customer_id, status,
    payment_timing, payment_method, items_list_subtotal_vnd,
    direct_sale_discount_vnd, items_subtotal_vnd, product_discount_vnd,
    order_discount_vnd, shipping_fee_vnd, shipping_discount_vnd, final_total_vnd,
    payment_due_at, payment_succeeded_at, shipment_delivered_at,
    cancel_reason, cancelled_at, confirmed_at, completed_at, created_at, updated_at
)
SELECT
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'SEED' || lpad(i::text, 8, '0'),
    ('d1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    order_status,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'PREPAID' ELSE 'POSTPAID' END,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'VNPAY' ELSE 'COD' END,
    unit_price * quantity, 0, unit_price * quantity, 0, 0, 30000, 0,
    unit_price * quantity + 30000,
    CASE WHEN order_status = 'PENDING_PAYMENT' THEN NOW() + INTERVAL '30 minutes' END,
    CASE WHEN order_status NOT IN ('PENDING_PAYMENT', 'CANCELLED') AND (i % 2 = 0 OR order_status IN ('DELIVERED', 'COMPLETED')) THEN NOW() - make_interval(days => 48 - i) END,
    CASE WHEN order_status IN ('DELIVERED', 'COMPLETED') THEN NOW() - make_interval(days => 47 - i) END,
    CASE WHEN order_status = 'CANCELLED' THEN 'CUSTOMER_REQUEST' END,
    CASE WHEN order_status = 'CANCELLED' THEN NOW() - make_interval(days => 48 - i) END,
    CASE WHEN order_status NOT IN ('PENDING_PAYMENT', 'CANCELLED') THEN NOW() - make_interval(days => 48 - i) END,
    CASE WHEN order_status = 'COMPLETED' THEN NOW() - make_interval(days => 46 - i) END,
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 46 - i)
FROM priced
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    payment_succeeded_at = EXCLUDED.payment_succeeded_at,
    shipment_delivered_at = EXCLUDED.shipment_delivered_at,
    cancel_reason = EXCLUDED.cancel_reason,
    cancelled_at = EXCLUDED.cancelled_at,
    confirmed_at = EXCLUDED.confirmed_at,
    completed_at = EXCLUDED.completed_at,
    updated_at = NOW();

INSERT INTO order_addresses (
    id, order_id, source_address_id, recipient_name, phone, address_line,
    province_id, ward_id, province_name, ward_name, created_at
)
SELECT
    ('d5000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a2000000-0000-4000-8000-' || lpad((((i - 1) % 12) + 1)::text, 12, '0'))::uuid,
    format('Khách hàng Seed %s', ((i - 1) % 12) + 1),
    '091' || lpad((((i - 1) % 12) + 1)::text, 7, '0'),
    format('%s Nguyễn Trãi, Thanh Xuân', 10 + (((i - 1) % 12) + 1)),
    201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW() - make_interval(days => 49 - i)
FROM generate_series(1, 48) AS s(i)
ON CONFLICT (id) DO NOTHING;

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO order_items (
    id, order_id, product_id, variant_id, sku, product_name, variant_name,
    image_url, list_price_vnd, direct_sale_discount_vnd, unit_price_vnd,
    quantity, product_discount_vnd, order_discount_vnd, line_total_vnd,
    weight_grams, created_at
)
SELECT
    ('d6000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((product_no - 1) * 3) + 1)::text, 12, '0'))::uuid,
    'DM-' || lpad(product_no::text, 3, '0') || '-1', 'Sản phẩm DynamicMart ' || lpad(product_no::text, 3, '0'), 'Tiêu chuẩn',
    CASE ((product_no - 1) % 6) + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1000&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1000&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1000&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=80'
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1000&q=80'
    END,
    79000 + product_no * 7000, 0, 79000 + product_no * 7000,
    quantity, 0, 0, (79000 + product_no * 7000) * quantity,
    250 + (product_no % 8) * 100, NOW() - make_interval(days => 49 - i)
FROM data
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;

INSERT INTO order_shipping_snapshots (
    id, order_id, quote_id, input_fingerprint, service_id, service_name,
    fee_vnd, eta, provider, shipping_discount_vnd, payable_fee_vnd,
    eta_text, total_weight_grams, package_length_cm, package_width_cm,
    package_height_cm, to_province_id, to_ward_id, to_province_name,
    to_ward_name, quoted_at, created_at
)
SELECT
    ('d7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('e3000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    repeat(substr(md5('seed-shipping-' || i), 1, 32), 2),
    53321, 'GHN Tiêu chuẩn', 30000, NOW() + INTERVAL '3 days', 'GHN', 0, 30000,
    'Giao trong 2-3 ngày', 1000, 25, 20, 10, 201, 11007, 'Hà Nội', 'Phường Phú Diễn',
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 49 - i)
FROM generate_series(1, 48) AS s(i)
ON CONFLICT (id) DO NOTHING;

WITH statuses AS (
    SELECT i,
           CASE (i % 8)
               WHEN 1 THEN 'PENDING_PAYMENT' WHEN 2 THEN 'CONFIRMED'
               WHEN 3 THEN 'PACKING' WHEN 4 THEN 'SHIPPING'
               WHEN 5 THEN 'HANDOVER_PENDING' WHEN 6 THEN 'DELIVERED'
               WHEN 7 THEN 'COMPLETED' ELSE 'CANCELLED'
           END AS order_status
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO order_status_history (id, order_id, from_status, to_status, actor_type, actor_id, reason, created_at)
SELECT
    ('d8000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    NULL, order_status,
    CASE WHEN order_status = 'CANCELLED' THEN 'CUSTOMER' ELSE 'SYSTEM' END,
    CASE WHEN order_status = 'CANCELLED' THEN ('a1000000-0000-4000-8000-' || lpad((((i - 1) % 12) + 1)::text, 12, '0'))::uuid END,
    'Dữ liệu trạng thái do seeder toàn hệ thống tạo',
    NOW() - make_interval(days => 49 - i)
FROM statuses
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_sagas (
    id, checkout_session_id, order_id, status, current_step,
    correlation_id, attempt_count, completed_at, payment_id,
    idempotency_key, request_hash, created_at, updated_at
)
SELECT
    ('d9000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'COMPLETED', 'ORDER_CREATED',
    ('da000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    0, NOW() - make_interval(days => 49 - i),
    ('e1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('db000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    repeat(substr(md5('seed-saga-' || i), 1, 32), 2),
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 49 - i)
FROM generate_series(1, 48) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = 'COMPLETED', current_step = 'ORDER_CREATED', updated_at = NOW();

SELECT 'order' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'd4000000-%') AS orders,
       (SELECT count(*) FROM order_items WHERE id::text LIKE 'd6000000-%') AS order_items,
       jsonb_object_agg(status, status_count ORDER BY status) AS statuses
FROM (
    SELECT id, status, count(*) OVER (PARTITION BY status) AS status_count
    FROM orders WHERE id::text LIKE 'd4000000-%'
) summary;
