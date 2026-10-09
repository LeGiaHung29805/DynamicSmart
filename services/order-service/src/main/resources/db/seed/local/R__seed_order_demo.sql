-- Two local demo orders: one completed VNPay order and one pending COD collection.
INSERT INTO checkout_sessions (
    id, customer_id, source, cart_id, selection_fingerprint, address_id, status,
    payment_timing, payment_method, items_list_subtotal_vnd, direct_sale_discount_vnd,
    items_subtotal_vnd, product_discount_vnd, order_discount_vnd,
    shipping_fee_vnd, shipping_discount_vnd, final_total_vnd,
    expires_at, completed_order_id, created_at, updated_at
)
VALUES
    ('80010000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'CART', '70000000-0000-4000-8000-000000000001', repeat('a', 64),
     '01000000-0000-4000-8000-000000000001', 'COMPLETED', 'PREPAID', 'VNPAY',
     15990000, 500000, 15490000, 0, 1000000, 30000, 30000, 14490000,
     NOW() - INTERVAL '8 days', '80000000-0000-4000-8000-000000000001',
     NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days'),
    ('80010000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     'CART', '70000000-0000-4000-8000-000000000001', repeat('b', 64),
     '01000000-0000-4000-8000-000000000001', 'COMPLETED', 'POSTPAID', 'COD',
     24990000, 0, 24990000, 0, 0, 40000, 0, 25030000,
     NOW() + INTERVAL '1 day', '80000000-0000-4000-8000-000000000002',
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO checkout_session_items (
    id, checkout_session_id, product_id, variant_id, sku, product_name, variant_name,
    image_url, list_price_vnd, direct_sale_promotion_id, direct_sale_discount_vnd,
    unit_price_vnd, quantity, weight_grams, length_cm, width_cm, height_cm, created_at
)
VALUES
    ('80020000-0000-4000-8000-000000000001', '80010000-0000-4000-8000-000000000001',
     '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'DPP-BLK-128', 'Dynamic Phone Pro', 'Đen / 128 GB',
     'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9', 15990000,
     '70200000-0000-4000-8000-000000000001', 500000, 15490000, 1, 420, 18, 10, 6,
     NOW() - INTERVAL '9 days'),
    ('80020000-0000-4000-8000-000000000002', '80010000-0000-4000-8000-000000000002',
     '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000003',
     'DLA-BLK-512', 'Dynamic Laptop Air', 'Đen / 512 GB',
     'https://images.unsplash.com/photo-1496181133206-80ce9b88a853', 24990000,
     NULL, 0, 24990000, 1, 1800, 42, 30, 8, NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO checkout_shipping_quotes (
    id, checkout_session_id, quote_id, input_fingerprint, fee_vnd, service_id,
    service_name, eta, expires_at, status, provider, shipping_discount_vnd,
    payable_fee_vnd, eta_text, total_weight_grams, package_length_cm,
    package_width_cm, package_height_cm, to_province_id, to_ward_id,
    to_province_name, to_ward_name, quoted_at, consumed_at, raw_response_redacted
)
VALUES
    ('80030000-0000-4000-8000-000000000001', '80010000-0000-4000-8000-000000000001',
     '83000000-0000-4000-8000-000000000001', repeat('c', 64), 30000, 53320,
     'GHN đường bộ', NOW() - INTERVAL '7 days', NOW() - INTERVAL '8 days', 'CONSUMED',
     'GHN', 30000, 0, 'Giao dự kiến 2-3 ngày', 420, 18, 10, 6,
     201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW() - INTERVAL '9 days',
     NOW() - INTERVAL '8 days', '{"demo":true}'::jsonb),
    ('80030000-0000-4000-8000-000000000002', '80010000-0000-4000-8000-000000000002',
     '83000000-0000-4000-8000-000000000002', repeat('d', 64), 40000, 53320,
     'GHN đường bộ', NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day', 'CONSUMED',
     'GHN', 0, 40000, 'Giao dự kiến 2-3 ngày', 1800, 42, 30, 8,
     201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW() - INTERVAL '1 day',
     NOW() - INTERVAL '1 day', '{"demo":true}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO orders (
    id, order_number, checkout_session_id, customer_id, status, payment_timing,
    payment_method, items_list_subtotal_vnd, direct_sale_discount_vnd,
    items_subtotal_vnd, product_discount_vnd, order_discount_vnd,
    shipping_fee_vnd, shipping_discount_vnd, final_total_vnd, currency,
    payment_succeeded_at, shipment_delivered_at, confirmed_at, completed_at,
    created_at, updated_at
)
VALUES
    ('80000000-0000-4000-8000-000000000001', 'DM-DEMO-0001',
     '80010000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'COMPLETED', 'PREPAID', 'VNPAY', 15990000, 500000, 15490000, 0, 1000000,
     30000, 30000, 14490000, 'VND', NOW() - INTERVAL '8 days', NOW() - INTERVAL '3 days',
     NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '9 days', NOW() - INTERVAL '2 days'),
    ('80000000-0000-4000-8000-000000000002', 'DM-DEMO-0002',
     '80010000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     'HANDOVER_PENDING', 'POSTPAID', 'COD', 24990000, 0, 24990000, 0, 0,
     40000, 0, 25030000, 'VND', NULL, NULL, NOW() - INTERVAL '1 day', NULL,
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO order_addresses (
    id, order_id, source_address_id, recipient_name, phone, address_line,
    province_id, ward_id, province_name, ward_name, created_at
)
VALUES
    ('80100000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
     '01000000-0000-4000-8000-000000000001', 'Khách hàng Demo', '0900000002',
     '41A Đường Phú Diễn, Phường Phú Diễn', 201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW() - INTERVAL '9 days'),
    ('80100000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002',
     '01000000-0000-4000-8000-000000000001', 'Khách hàng Demo', '0900000002',
     '41A Đường Phú Diễn, Phường Phú Diễn', 201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO order_items (
    id, order_id, product_id, variant_id, sku, product_name, variant_name, image_url,
    list_price_vnd, direct_sale_promotion_id, direct_sale_discount_vnd, unit_price_vnd,
    quantity, product_discount_vnd, order_discount_vnd, line_total_vnd, weight_grams, created_at
)
VALUES
    ('80200000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
     '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
     'DPP-BLK-128', 'Dynamic Phone Pro', 'Đen / 128 GB',
     'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9', 15990000,
     '70200000-0000-4000-8000-000000000001', 500000, 15490000, 1, 0, 1000000, 14490000, 420,
     NOW() - INTERVAL '9 days'),
    ('80200000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002',
     '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000003',
     'DLA-BLK-512', 'Dynamic Laptop Air', 'Đen / 512 GB',
     'https://images.unsplash.com/photo-1496181133206-80ce9b88a853', 24990000,
     NULL, 0, 24990000, 1, 0, 0, 24990000, 1800, NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

INSERT INTO order_voucher_snapshots (
    id, order_id, voucher_id, voucher_code, scope, discount_method,
    discount_value, eligible_subtotal_vnd, discount_amount_vnd, shipping_discount_vnd
)
VALUES
    ('80300000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
     '71000000-0000-4000-8000-000000000001', 'WELCOME10', 'ORDER_DISCOUNT',
     'PERCENTAGE', 1000, 15490000, 1000000, 0),
    ('80300000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000001',
     '70000000-0000-0000-0000-000000000002', 'FREESHIP50', 'SHIPPING_DISCOUNT',
     'FIXED_AMOUNT', 50000, 30000, 0, 30000)
ON CONFLICT DO NOTHING;

INSERT INTO order_shipping_snapshots (
    id, order_id, quote_id, input_fingerprint, service_id, service_name, fee_vnd,
    eta, provider, source_checkout_quote_id, shipping_discount_vnd, payable_fee_vnd,
    eta_text, total_weight_grams, package_length_cm, package_width_cm, package_height_cm,
    to_province_id, to_ward_id, to_province_name, to_ward_name, quoted_at, raw_response_redacted
)
VALUES
    ('80400000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
     '83000000-0000-4000-8000-000000000001', repeat('c', 64), 53320, 'GHN đường bộ', 30000,
     NOW() - INTERVAL '7 days', 'GHN', '80030000-0000-4000-8000-000000000001', 30000, 0,
     'Giao dự kiến 2-3 ngày', 420, 18, 10, 6, 201, 11007, 'Hà Nội', 'Phường Phú Diễn',
     NOW() - INTERVAL '9 days', '{"demo":true}'::jsonb),
    ('80400000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002',
     '83000000-0000-4000-8000-000000000002', repeat('d', 64), 53320, 'GHN đường bộ', 40000,
     NOW() + INTERVAL '1 day', 'GHN', '80030000-0000-4000-8000-000000000002', 0, 40000,
     'Giao dự kiến 2-3 ngày', 1800, 42, 30, 8, 201, 11007, 'Hà Nội', 'Phường Phú Diễn',
     NOW() - INTERVAL '1 day', '{"demo":true}'::jsonb)
ON CONFLICT DO NOTHING;

INSERT INTO order_status_history (
    id, order_id, from_status, to_status, actor_type, actor_id, reason, created_at, correlation_id
)
VALUES
    ('80500000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', NULL,
     'PENDING_PAYMENT', 'SYSTEM', NULL, 'Tạo đơn demo', NOW() - INTERVAL '9 days',
     '89000000-0000-4000-8000-000000000001'),
    ('80500000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000001',
     'PENDING_PAYMENT', 'CONFIRMED', 'PAYMENT_EVENT', NULL, 'VNPay thành công', NOW() - INTERVAL '8 days',
     '89000000-0000-4000-8000-000000000001'),
    ('80500000-0000-4000-8000-000000000003', '80000000-0000-4000-8000-000000000001',
     'DELIVERED', 'COMPLETED', 'SYSTEM', NULL, 'Hoàn thành đơn demo', NOW() - INTERVAL '2 days',
     '89000000-0000-4000-8000-000000000001'),
    ('80500000-0000-4000-8000-000000000004', '80000000-0000-4000-8000-000000000002',
     'PACKING', 'HANDOVER_PENDING', 'ADMIN', '00000000-0000-4000-8000-000000000001',
     'Chờ thu COD', NOW() - INTERVAL '12 hours', '89000000-0000-4000-8000-000000000002')
ON CONFLICT DO NOTHING;
