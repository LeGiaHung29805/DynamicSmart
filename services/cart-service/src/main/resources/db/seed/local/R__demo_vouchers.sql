INSERT INTO vouchers (
    id, code, name, description, status, scope, discount_method,
    fixed_discount_vnd, discount_rate_bps, max_discount_vnd,
    minimum_order_vnd, minimum_eligible_subtotal_vnd,
    usage_limit, consumed_count, usage_limit_per_customer,
    starts_at, ends_at, distribution_mode, is_default, created_by
) VALUES
(
    '70000000-0000-0000-0000-000000000001', 'DYNAMIC10', 'Giảm 10% toàn đơn',
    'Giảm 10% cho đơn từ 500.000đ, tối đa 200.000đ.', 'ACTIVE', 'ORDER_DISCOUNT', 'PERCENTAGE',
    NULL, 1000, 200000, 500000, 0, 10000, 0, 1,
    '2025-01-01T00:00:00Z', '2030-12-31T23:59:59Z', 'DEFAULT_FOR_ELIGIBLE', TRUE,
    '00000000-0000-4000-8000-000000000001'
),
(
    '70000000-0000-0000-0000-000000000002', 'FREESHIP50', 'Miễn phí vận chuyển',
    'Giảm tối đa 50.000đ phí giao hàng cho đơn từ 300.000đ.', 'ACTIVE', 'SHIPPING_DISCOUNT', 'FIXED_AMOUNT',
    50000, NULL, NULL, 300000, 0, 10000, 0, 1,
    '2025-01-01T00:00:00Z', '2030-12-31T23:59:59Z', 'DEFAULT_FOR_ELIGIBLE', TRUE,
    '00000000-0000-4000-8000-000000000001'
)
ON CONFLICT (code) DO NOTHING;
