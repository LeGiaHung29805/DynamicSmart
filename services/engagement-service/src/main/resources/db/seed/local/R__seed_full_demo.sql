-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

INSERT INTO wishlists (id, customer_id, created_at, updated_at)
SELECT
    ('f1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    NOW() - make_interval(days => 20 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET updated_at = NOW();

WITH items AS (
    SELECT customer_no, slot,
           (((customer_no * 9 + slot) - 1) % 120) + 1 AS product_no
    FROM generate_series(1, 12) AS customers(customer_no)
    CROSS JOIN generate_series(1, 4) AS slots(slot)
)
INSERT INTO wishlist_items (id, wishlist_id, product_id, created_at)
SELECT
    ('f2000000-0000-4000-8000-' || lpad(((customer_no - 1) * 4 + slot)::text, 12, '0'))::uuid,
    ('f1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    NOW() - make_interval(days => customer_no + slot)
FROM items
ON CONFLICT (id) DO NOTHING;

-- Only COMPLETED orders are reviewable: order sequence 7, 15, 23, 31, 39, 47.
WITH completed AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           ((i - 1) % 12) + 1 AS customer_no,
           row_number() OVER (ORDER BY i) AS review_no
    FROM generate_series(7, 47, 8) AS s(i)
)
INSERT INTO reviews (
    id, order_item_id, order_id, customer_id, product_id, variant_id,
    rating, content, status, created_at, updated_at
)
SELECT
    ('f3000000-0000-4000-8000-' || lpad(review_no::text, 12, '0'))::uuid,
    ('d6000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(product_no::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((product_no - 1) * 3) + 1)::text, 12, '0'))::uuid,
    4 + (review_no % 2),
    format('Sản phẩm đúng mô tả, đóng gói tốt. Đánh giá mẫu #%s.', review_no),
    'VISIBLE', NOW() - make_interval(days => 48 - i), NOW() - make_interval(days => 48 - i)
FROM completed
ON CONFLICT (id) DO UPDATE SET rating = EXCLUDED.rating, content = EXCLUDED.content, status = 'VISIBLE', updated_at = NOW();

WITH completed AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           row_number() OVER (ORDER BY i) AS review_no
    FROM generate_series(7, 47, 8) AS s(i)
)
INSERT INTO review_images (id, review_id, image_url, sort_order, created_at)
SELECT
    ('fa000000-0000-4000-8000-' || lpad(review_no::text, 12, '0'))::uuid,
    ('f3000000-0000-4000-8000-' || lpad(review_no::text, 12, '0'))::uuid,
    CASE ((product_no - 1) % 6) + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1200&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1200&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1200&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1200&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1200&q=80'
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1200&q=80'
    END,
    0,
    NOW() - make_interval(days => 48 - i)
FROM completed
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;

INSERT INTO product_questions (id, product_id, customer_id, content, status, created_at, updated_at)
SELECT
    ('f4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad((((i * 5 - 1) % 120) + 1)::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    format('Sản phẩm này có bảo hành và đổi trả như thế nào? Câu hỏi mẫu %s.', i),
    'ANSWERED', NOW() - make_interval(days => 15 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = 'ANSWERED', updated_at = NOW();

INSERT INTO product_answers (id, question_id, admin_id, content, created_at, updated_at)
SELECT
    ('f5000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('f4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    '00000000-0000-4000-8000-000000000001',
    'DynamicMart hỗ trợ đổi trả theo chính sách hiển thị trên trang sản phẩm và đơn hàng.',
    NOW() - make_interval(days => 14 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, updated_at = NOW();

WITH statuses AS (
    SELECT i,
           CASE (i % 8)
               WHEN 1 THEN 'Chờ thanh toán' WHEN 2 THEN 'Đã xác nhận'
               WHEN 3 THEN 'Đang đóng gói' WHEN 4 THEN 'Đang giao hàng'
               WHEN 5 THEN 'Chờ bàn giao' WHEN 6 THEN 'Đã giao hàng'
               WHEN 7 THEN 'Đã hoàn tất' ELSE 'Đã hủy'
           END AS status_text
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO notifications (id, customer_id, type, title, content, read_at, created_at)
SELECT
    ('f6000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad((((i - 1) % 12) + 1)::text, 12, '0'))::uuid,
    'ORDER_STATUS',
    'Đơn SEED' || lpad(i::text, 8, '0') || ': ' || status_text,
    'Đơn hàng mẫu SEED' || lpad(i::text, 8, '0') || ' hiện ở trạng thái ' || status_text || '.',
    CASE WHEN i % 3 <> 0 THEN NOW() - make_interval(days => 48 - i) END,
    NOW() - make_interval(days => 49 - i)
FROM statuses
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, content = EXCLUDED.content;

INSERT INTO chat_conversations (id, customer_id, assigned_admin_id, status, last_message_at, created_at, updated_at)
SELECT
    ('f7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    '00000000-0000-4000-8000-000000000001',
    CASE WHEN i % 2 = 0 THEN 'CLOSED' ELSE 'OPEN' END,
    NOW() - make_interval(hours => i), NOW() - make_interval(days => i), NOW()
FROM generate_series(1, 6) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, last_message_at = EXCLUDED.last_message_at, updated_at = NOW();

INSERT INTO chat_messages (id, conversation_id, sender_id, sender_role, content, read_at, created_at, idempotency_key)
SELECT
    ('f8000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('f7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'CUSTOMER', 'Tôi cần hỗ trợ kiểm tra trạng thái đơn hàng mẫu.',
    CASE WHEN i % 2 = 0 THEN NOW() - make_interval(hours => i) END,
    NOW() - make_interval(hours => i),
    ('fb000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 6) AS s(i)
ON CONFLICT (id) DO NOTHING;

INSERT INTO daily_sales_metrics (
    metric_date, gross_item_sales_vnd, discount_value_vnd, shipping_fee_vnd,
    net_revenue_vnd, order_count, completed_order_count, updated_at
)
SELECT
    CURRENT_DATE - i,
    3000000 + i * 125000,
    100000 + i * 5000,
    180000,
    3080000 + i * 120000,
    6, 4 + (i % 3), NOW()
FROM generate_series(0, 29) AS s(i)
ON CONFLICT (metric_date) DO UPDATE SET
    gross_item_sales_vnd = EXCLUDED.gross_item_sales_vnd,
    discount_value_vnd = EXCLUDED.discount_value_vnd,
    shipping_fee_vnd = EXCLUDED.shipping_fee_vnd,
    net_revenue_vnd = EXCLUDED.net_revenue_vnd,
    order_count = EXCLUDED.order_count,
    completed_order_count = EXCLUDED.completed_order_count,
    updated_at = NOW();

INSERT INTO daily_product_metrics (
    metric_date, product_id, variant_id, quantity_sold,
    gross_sales_vnd, net_item_sales_vnd, review_count, rating_sum, updated_at
)
SELECT
    CURRENT_DATE - (i % 30),
    ('b2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad((((i - 1) * 3) + 1)::text, 12, '0'))::uuid,
    2 + (i % 8),
    (79000 + i * 7000) * (2 + (i % 8)),
    (79000 + i * 7000) * (2 + (i % 8)),
    CASE WHEN i % 8 = 0 THEN 1 ELSE 0 END,
    CASE WHEN i % 8 = 0 THEN 5 ELSE 0 END,
    NOW()
FROM generate_series(1, 120) AS s(i)
ON CONFLICT (metric_date, product_id, variant_id) DO UPDATE SET
    quantity_sold = EXCLUDED.quantity_sold,
    gross_sales_vnd = EXCLUDED.gross_sales_vnd,
    net_item_sales_vnd = EXCLUDED.net_item_sales_vnd,
    review_count = EXCLUDED.review_count,
    rating_sum = EXCLUDED.rating_sum,
    updated_at = NOW();

SELECT 'engagement' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'f1000000-%') AS wishlists,
       (SELECT count(*) FROM wishlist_items WHERE id::text LIKE 'f2000000-%') AS wishlist_items,
       (SELECT count(*) FROM reviews WHERE id::text LIKE 'f3000000-%') AS reviews,
       (SELECT count(*) FROM review_images WHERE id::text LIKE 'fa000000-%') AS review_images,
       (SELECT count(*) FROM notifications WHERE id::text LIKE 'f6000000-%') AS notifications
FROM wishlists;
