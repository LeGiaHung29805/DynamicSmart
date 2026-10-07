\set ON_ERROR_STOP on

\connect identity_db
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM users WHERE email_normalized LIKE '%@dynamicmart.local') < 3 THEN
        RAISE EXCEPTION 'identity_db thiếu tài khoản demo';
    END IF;
    IF (SELECT COUNT(*) FROM addresses WHERE user_id = '00000000-0000-4000-8000-000000000002') < 1 THEN
        RAISE EXCEPTION 'identity_db thiếu địa chỉ của customer demo';
    END IF;
END $$;
SELECT 'identity_db' AS database_name,
       (SELECT COUNT(*) FROM users WHERE email_normalized LIKE '%@dynamicmart.local') AS demo_users,
       (SELECT COUNT(*) FROM addresses) AS addresses;

\connect catalog_db
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM products WHERE id IN (
        '40000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000002')) <> 2 THEN
        RAISE EXCEPTION 'catalog_db thiếu sản phẩm demo chuẩn';
    END IF;
    IF (SELECT COUNT(*) FROM inventory_items WHERE variant_id IN (
        '50000000-0000-0000-0000-000000000001',
        '50000000-0000-0000-0000-000000000002',
        '50000000-0000-0000-0000-000000000003')) <> 3 THEN
        RAISE EXCEPTION 'catalog_db thiếu tồn kho demo';
    END IF;
END $$;
SELECT 'catalog_db' AS database_name,
       (SELECT COUNT(*) FROM products) AS products,
       (SELECT COUNT(*) FROM product_variants) AS variants,
       (SELECT COUNT(*) FROM inventory_items) AS inventory_items;

\connect cart_db
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM vouchers WHERE code = 'WELCOME10') OR
       NOT EXISTS (SELECT 1 FROM vouchers WHERE code = 'FREESHIP50') THEN
        RAISE EXCEPTION 'cart_db thiếu voucher demo';
    END IF;
END $$;
SELECT 'cart_db' AS database_name,
       (SELECT COUNT(*) FROM carts) AS carts,
       (SELECT COUNT(*) FROM cart_items) AS cart_items,
       (SELECT COUNT(*) FROM vouchers) AS vouchers;

\connect order_db
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM orders WHERE order_number IN ('DM-DEMO-0001', 'DM-DEMO-0002')) <> 2 THEN
        RAISE EXCEPTION 'order_db thiếu hai đơn hàng demo';
    END IF;
END $$;
SELECT 'order_db' AS database_name,
       (SELECT COUNT(*) FROM orders) AS orders,
       (SELECT COUNT(*) FROM order_items) AS order_items,
       (SELECT COUNT(*) FROM order_status_history) AS status_history;

\connect payment_db
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM payments WHERE id IN (
        '90000000-0000-4000-8000-000000000001',
        '90000000-0000-4000-8000-000000000002')) <> 2 THEN
        RAISE EXCEPTION 'payment_db thiếu payment demo';
    END IF;
END $$;
SELECT 'payment_db' AS database_name,
       (SELECT COUNT(*) FROM payments) AS payments,
       (SELECT COUNT(*) FROM payment_attempts) AS attempts,
       (SELECT COUNT(*) FROM ghn_location_wards) AS ghn_wards;

\connect engagement_db
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM reviews WHERE id = 'a1000000-0000-4000-8000-000000000001') THEN
        RAISE EXCEPTION 'engagement_db thiếu review demo';
    END IF;
END $$;
SELECT 'engagement_db' AS database_name,
       (SELECT COUNT(*) FROM reviews) AS reviews,
       (SELECT COUNT(*) FROM wishlist_items) AS wishlist_items,
       (SELECT COUNT(*) FROM notifications) AS notifications,
       (SELECT COUNT(*) FROM chat_messages) AS chat_messages;

\echo 'SEED VERIFY PASS: 6 database local có dữ liệu demo cốt lõi.'
