-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

INSERT INTO carts (id, customer_id, status, created_at, updated_at)
SELECT
    ('c1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'ACTIVE', NOW() - make_interval(days => 12 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = 'ACTIVE', updated_at = NOW();

WITH items AS (
    SELECT customer_no, slot,
           (((customer_no * 7 + slot) - 1) % 120) + 1 AS product_no
    FROM generate_series(1, 12) AS customers(customer_no)
    CROSS JOIN generate_series(1, 3) AS slots(slot)
)
INSERT INTO cart_items (id, cart_id, product_id, variant_id, quantity, version, is_selected, created_at, updated_at)
SELECT
    ('c2000000-0000-4000-8000-' || lpad(((customer_no - 1) * 3 + slot)::text, 12, '0'))::uuid,
    ('c1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((product_no - 1) * 3) + slot)::text, 12, '0'))::uuid,
    1 + ((customer_no + slot) % 3), 0, slot <> 3, NOW() - make_interval(hours => customer_no), NOW()
FROM items
ON CONFLICT (id) DO UPDATE SET
    quantity = EXCLUDED.quantity,
    is_selected = EXCLUDED.is_selected,
    updated_at = NOW();

INSERT INTO direct_price_promotions (
    id, name, description, status, discount_method, fixed_discount_vnd,
    discount_rate_bps, max_discount_vnd, starts_at, ends_at, created_by
)
VALUES
    ('c3000000-0000-4000-8000-000000000001', 'Giảm trực tiếp 20.000đ', 'Áp dụng cho nhóm sản phẩm seed đầu tiên', 'ACTIVE', 'FIXED_AMOUNT', 20000, NULL, NULL, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', '00000000-0000-4000-8000-000000000001'),
    ('c3000000-0000-4000-8000-000000000002', 'Giảm trực tiếp 10%', 'Khuyến mãi phần trăm có giới hạn', 'ACTIVE', 'PERCENTAGE', NULL, 1000, 100000, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', '00000000-0000-4000-8000-000000000001')
ON CONFLICT (id) DO UPDATE SET status = 'ACTIVE', starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, updated_at = NOW();

INSERT INTO direct_price_promotion_variants (promotion_id, variant_id)
SELECT
    ('c3000000-0000-4000-8000-' || lpad((1 + (i % 2))::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((i - 1) * 3) + 1)::text, 12, '0'))::uuid
FROM generate_series(1, LEAST(120, 30)) AS s(i)
ON CONFLICT DO NOTHING;

INSERT INTO vouchers (
    id, code, name, description, status, scope, discount_method,
    fixed_discount_vnd, discount_rate_bps, max_discount_vnd,
    minimum_order_vnd, minimum_eligible_subtotal_vnd,
    usage_limit, consumed_count, usage_limit_per_customer,
    starts_at, ends_at, distribution_mode, is_default, created_by
)
VALUES
    ('c4000000-0000-4000-8000-000000000001', 'SEEDWELCOME50', 'Chào mừng 50K', 'Giảm 50.000đ cho đơn từ 300.000đ', 'ACTIVE', 'ORDER_DISCOUNT', 'FIXED_AMOUNT', 50000, NULL, NULL, 300000, NULL, 10000, 0, 1, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 'DEFAULT_FOR_ELIGIBLE', TRUE, '00000000-0000-4000-8000-000000000001'),
    ('c4000000-0000-4000-8000-000000000002', 'SEEDSHIP30', 'Miễn phí vận chuyển 30K', 'Giảm phí vận chuyển tối đa 30.000đ', 'ACTIVE', 'SHIPPING_DISCOUNT', 'FIXED_AMOUNT', 30000, NULL, NULL, 200000, NULL, 10000, 0, 3, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 'CODE_ONLY', FALSE, '00000000-0000-4000-8000-000000000001'),
    ('c4000000-0000-4000-8000-000000000003', 'SEEDSALE10', 'Giảm 10%', 'Giảm 10%, tối đa 100.000đ', 'ACTIVE', 'ORDER_DISCOUNT', 'PERCENTAGE', NULL, 1000, 100000, 500000, NULL, 5000, 0, 2, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 'CODE_ONLY', FALSE, '00000000-0000-4000-8000-000000000001'),
    ('c4000000-0000-4000-8000-000000000004', 'SEEDVIP15', 'Ưu đãi VIP 15%', 'Voucher được gán cho khách seed', 'ACTIVE', 'ORDER_DISCOUNT', 'PERCENTAGE', NULL, 1500, 150000, 700000, NULL, 1000, 0, 1, NOW() - INTERVAL '30 days', NOW() + INTERVAL '180 days', 'ASSIGNED_ONLY', FALSE, '00000000-0000-4000-8000-000000000001')
ON CONFLICT (id) DO UPDATE SET status = 'ACTIVE', starts_at = EXCLUDED.starts_at, ends_at = EXCLUDED.ends_at, updated_at = NOW();

INSERT INTO customer_vouchers (id, customer_id, voucher_id, status, expires_at, source, assigned_by)
SELECT
    ('c5000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'c4000000-0000-4000-8000-000000000004',
    'AVAILABLE', NOW() + INTERVAL '180 days', 'ADMIN_ASSIGNMENT', '00000000-0000-4000-8000-000000000001'
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = 'AVAILABLE', expires_at = EXCLUDED.expires_at;

SELECT 'cart' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'c1000000-%') AS carts,
       (SELECT count(*) FROM cart_items WHERE id::text LIKE 'c2000000-%') AS cart_items,
       (SELECT count(*) FROM vouchers WHERE id::text LIKE 'c4000000-%') AS vouchers
FROM carts;
