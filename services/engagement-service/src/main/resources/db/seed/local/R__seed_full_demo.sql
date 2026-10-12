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
    CASE review_no
        WHEN 1 THEN 'Tai nghe kết nối nhanh, đeo khoảng hai giờ vẫn êm tai. Hộp đóng gói chắc chắn và giao đúng hẹn.'
        WHEN 2 THEN 'Áo đúng phom như mô tả, vải mềm và không bị bai sau lần giặt đầu tiên. Sẽ quay lại ủng hộ.'
        WHEN 3 THEN 'Đèn đủ sáng cho bàn học, ba mức sáng dễ dùng. Nhân viên tư vấn cũng phản hồi rất nhanh.'
        WHEN 4 THEN 'Sữa rửa mặt dịu nhẹ, da không bị khô căng. Sản phẩm còn nguyên niêm phong khi nhận hàng.'
        WHEN 5 THEN 'Bình nước kín, bỏ trong balo không bị rò. Dung tích đúng nhu cầu tập luyện của mình.'
        ELSE 'Sổ giấy dày, viết bút mực không lem sang trang sau. Giao hàng cẩn thận và sản phẩm sạch đẹp.'
    END,
    'VISIBLE', NOW() - make_interval(days => 48 - i), NOW() - make_interval(days => 48 - i)
FROM completed
ON CONFLICT (id) DO UPDATE SET rating = EXCLUDED.rating, content = EXCLUDED.content, status = 'VISIBLE', updated_at = NOW();

-- A review record for every named team customer (identity customer 1..5).
-- These IDs are deliberately separate from the general review dataset above so the seed remains repeatable.
INSERT INTO reviews (
    id, order_item_id, order_id, customer_id, product_id, variant_id,
    rating, content, status, created_at, updated_at
)
VALUES
    ('f3100000-0000-4000-8000-000000000001', 'd6000000-0000-4000-8000-000000000001', 'd4000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'b2000000-0000-4000-8000-000000000002', 'b3000000-0000-4000-8000-000000000004', 5, 'Sản phẩm đúng mô tả, đóng gói cẩn thận và thao tác đặt hàng dễ theo dõi.', 'VISIBLE', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
    ('f3100000-0000-4000-8000-000000000002', 'd6000000-0000-4000-8000-000000000002', 'd4000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000002', 'b2000000-0000-4000-8000-000000000004', 'b3000000-0000-4000-8000-000000000010', 4, 'Chất lượng phù hợp mức giá, giao hàng có cập nhật trạng thái rõ ràng.', 'VISIBLE', NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
    ('f3100000-0000-4000-8000-000000000003', 'd6000000-0000-4000-8000-000000000003', 'd4000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000003', 'b2000000-0000-4000-8000-000000000006', 'b3000000-0000-4000-8000-000000000016', 5, 'Ảnh và thông số hiển thị đầy đủ, biến thể được chọn đúng khi nhận hàng.', 'VISIBLE', NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
    ('f3100000-0000-4000-8000-000000000004', 'd6000000-0000-4000-8000-000000000004', 'd4000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000004', 'b2000000-0000-4000-8000-000000000008', 'b3000000-0000-4000-8000-000000000022', 4, 'Mã giảm giá được áp dụng đúng và tổng tiền cập nhật ngay tại bước thanh toán.', 'VISIBLE', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
    ('f3100000-0000-4000-8000-000000000005', 'd6000000-0000-4000-8000-000000000005', 'd4000000-0000-4000-8000-000000000005', 'a1000000-0000-4000-8000-000000000005', 'b2000000-0000-4000-8000-000000000010', 'b3000000-0000-4000-8000-000000000028', 5, 'Nhân viên hỗ trợ phản hồi nhanh và thông tin đơn hàng rất dễ kiểm tra.', 'VISIBLE', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days')
ON CONFLICT (id) DO UPDATE SET rating = EXCLUDED.rating, content = EXCLUDED.content, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at;

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
    CASE (i - 1) % 4
        WHEN 0 THEN 'Sản phẩm này có những lựa chọn biến thể nào và hình thực tế có giống mô tả không?'
        WHEN 1 THEN 'Nếu nhận hàng không vừa hoặc không phù hợp, tôi có thể đổi trả trong bao lâu?'
        WHEN 2 THEN 'Sản phẩm có bảo hành không và cần lưu ý gì khi sử dụng hằng ngày?'
        ELSE 'Đơn hàng thường được giao trong mấy ngày tại Hà Nội và có thể theo dõi ở đâu?'
    END,
    'ANSWERED', NOW() - make_interval(days => 15 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET status = 'ANSWERED', updated_at = NOW();

INSERT INTO product_answers (id, question_id, admin_id, content, created_at, updated_at)
SELECT
    ('f5000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('f4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    '00000000-0000-4000-8000-000000000001',
    CASE (i - 1) % 4
        WHEN 0 THEN 'Mỗi biến thể có ảnh và thông số riêng trên trang sản phẩm. Bạn có thể chọn biến thể trước khi thêm vào giỏ.'
        WHEN 1 THEN 'DynamicMart hỗ trợ đổi trả trong 7 ngày khi sản phẩm còn nguyên điều kiện theo chính sách hiển thị trên đơn hàng.'
        WHEN 2 THEN 'Thông tin bảo hành và cách sử dụng được ghi tại mục thông số sản phẩm; bạn vui lòng giữ lại mã đơn để được hỗ trợ.'
        ELSE 'Thời gian giao dự kiến được GHN hiển thị ở checkout và bạn có thể theo dõi tiến trình trong mục Đơn hàng của tôi.'
    END,
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
    'CUSTOMER', CASE i
        WHEN 1 THEN 'Tôi muốn kiểm tra dự kiến giao hàng của đơn này.'
        WHEN 2 THEN 'Sản phẩm nhận được có thể đổi sang biến thể khác không?'
        WHEN 3 THEN 'Tôi cần hướng dẫn dùng mã giảm giá cho đơn tiếp theo.'
        WHEN 4 THEN 'Xin cho biết thời gian bảo hành của sản phẩm đã mua.'
        WHEN 5 THEN 'Tôi muốn hỏi cách theo dõi đơn hàng trên website.'
        ELSE 'Tôi cần hỗ trợ xác nhận thông tin thanh toán của đơn.'
    END,
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

-- Seed agent replies so the customer-support screen has complete conversations.
INSERT INTO chat_messages (id, conversation_id, sender_id, sender_role, content, read_at, created_at, idempotency_key)
SELECT
    ('f8100000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('f7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    '00000000-0000-4000-8000-000000000001',
    'ADMIN',
    CASE i
        WHEN 1 THEN 'Chào bạn, đơn hàng đang được đối soát với đơn vị giao hàng. Thời gian dự kiến luôn hiển thị trong chi tiết đơn.'
        WHEN 2 THEN 'Bạn có thể gửi yêu cầu đổi trả từ chi tiết đơn hàng trong thời hạn 7 ngày nếu sản phẩm đáp ứng điều kiện.'
        WHEN 3 THEN 'Voucher hợp lệ sẽ sáng màu ở checkout. Sau khi chọn, tổng tiền được tính lại ngay ở phần tóm tắt.'
        WHEN 4 THEN 'Thông tin bảo hành và hướng dẫn sử dụng nằm trong phần Thông số nổi bật của từng sản phẩm.'
        WHEN 5 THEN 'Bạn vào Tài khoản > Đơn hàng để xem trạng thái, lịch sử xử lý và mã vận đơn nếu đã có.'
        ELSE 'Chúng tôi đã ghi nhận yêu cầu. Nếu giao dịch đã trừ tiền nhưng chưa tạo đơn, CSKH sẽ hướng dẫn bạn làm việc với quản trị viên.'
    END,
    NOW() - make_interval(hours => i - 1),
    NOW() - make_interval(hours => i - 1),
    ('fb100000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid
FROM generate_series(1, 6) AS s(i)
ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content, read_at = EXCLUDED.read_at, created_at = EXCLUDED.created_at;
