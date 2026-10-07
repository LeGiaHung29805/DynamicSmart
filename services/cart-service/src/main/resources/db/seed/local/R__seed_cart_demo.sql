-- Idempotent local demo cart, promotion and vouchers.
INSERT INTO carts (id, customer_id, status)
VALUES ('70000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002', 'ACTIVE')
ON CONFLICT DO NOTHING;

WITH demo_cart AS (
    SELECT id
    FROM carts
    WHERE customer_id = '00000000-0000-4000-8000-000000000002'
      AND status = 'ACTIVE'
    ORDER BY updated_at DESC
    LIMIT 1
)
INSERT INTO cart_items (id, cart_id, product_id, variant_id, quantity, version, is_selected)
SELECT seed.id, demo_cart.id, seed.product_id, seed.variant_id, seed.quantity, seed.version, seed.is_selected
FROM demo_cart
CROSS JOIN (VALUES
    ('70100000-0000-4000-8000-000000000001'::UUID,
     '40000000-0000-0000-0000-000000000001'::UUID, '50000000-0000-0000-0000-000000000001'::UUID, 1, 0, TRUE),
    ('70100000-0000-4000-8000-000000000002'::UUID,
     '40000000-0000-0000-0000-000000000002'::UUID, '50000000-0000-0000-0000-000000000003'::UUID, 1, 0, TRUE)
) AS seed(id, product_id, variant_id, quantity, version, is_selected)
ON CONFLICT DO NOTHING;

INSERT INTO direct_price_promotions (
    id, name, description, status, discount_method, fixed_discount_vnd,
    starts_at, ends_at, created_by
)
VALUES (
    '70200000-0000-4000-8000-000000000001', 'Giảm trực tiếp điện thoại demo',
    'Dữ liệu local dùng để kiểm tra giá khuyến mãi.', 'ACTIVE', 'FIXED_AMOUNT', 500000,
    NOW() - INTERVAL '30 days', NOW() + INTERVAL '365 days',
    '00000000-0000-4000-8000-000000000001'
)
ON CONFLICT DO NOTHING;

INSERT INTO direct_price_promotion_variants (promotion_id, variant_id)
VALUES ('70200000-0000-4000-8000-000000000001', '50000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO vouchers (
    id, code, name, description, status, scope, discount_method,
    fixed_discount_vnd, discount_rate_bps, max_discount_vnd,
    minimum_order_vnd, usage_limit, usage_limit_per_customer,
    starts_at, ends_at, distribution_mode, is_default, created_by
)
VALUES
    ('71000000-0000-4000-8000-000000000001', 'WELCOME10', 'Giảm 10% đơn hàng',
     'Voucher demo giảm tối đa 1.000.000đ.', 'ACTIVE', 'ORDER_DISCOUNT', 'PERCENTAGE',
     NULL, 1000, 1000000, 1000000, 1000, 1,
     NOW() - INTERVAL '30 days', NOW() + INTERVAL '365 days', 'ASSIGNED_ONLY', FALSE,
     '00000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO customer_vouchers (id, customer_id, voucher_id, status, expires_at, source, assigned_by)
VALUES
    ('71100000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     '71000000-0000-4000-8000-000000000001', 'AVAILABLE', NOW() + INTERVAL '365 days',
     'ADMIN_ASSIGNMENT', '00000000-0000-4000-8000-000000000001'),
    ('71100000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     COALESCE((SELECT id FROM vouchers WHERE code = 'FREESHIP50'),
              '70000000-0000-0000-0000-000000000002'), 'AVAILABLE', NOW() + INTERVAL '365 days',
     'ADMIN_ASSIGNMENT', '00000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;
