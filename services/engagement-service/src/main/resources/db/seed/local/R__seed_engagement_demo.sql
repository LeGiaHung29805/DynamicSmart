-- Local demo review, wishlist, notification, Q&A, chat and reporting data.
INSERT INTO wishlists (id, customer_id)
VALUES ('a0000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002')
ON CONFLICT DO NOTHING;

INSERT INTO wishlist_items (id, wishlist_id, product_id)
VALUES
    ('a0100000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
     '40000000-0000-0000-0000-000000000001'),
    ('a0100000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
     '40000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;

INSERT INTO reviews (
    id, order_item_id, order_id, customer_id, product_id, variant_id,
    rating, content, status, created_at, updated_at
)
VALUES (
    'a1000000-0000-4000-8000-000000000001', '80200000-0000-4000-8000-000000000001',
    '80000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
    '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001',
    5, 'Sản phẩm demo đẹp, giao hàng nhanh và đóng gói cẩn thận.', 'VISIBLE',
    NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
)
ON CONFLICT DO NOTHING;

INSERT INTO review_images (id, review_id, image_url, sort_order)
VALUES (
    'a1100000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001',
    'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9', 0
)
ON CONFLICT DO NOTHING;

INSERT INTO product_questions (id, product_id, customer_id, content, status)
VALUES (
    'a2000000-0000-4000-8000-000000000001', '40000000-0000-0000-0000-000000000002',
    '00000000-0000-4000-8000-000000000002', 'Laptop có phù hợp cho sinh viên lập trình không?', 'ANSWERED'
)
ON CONFLICT DO NOTHING;

INSERT INTO product_answers (id, question_id, admin_id, content)
VALUES (
    'a2100000-0000-4000-8000-000000000001', 'a2000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000001',
    'Máy phù hợp cho học tập và lập trình; bạn có thể nâng cấu hình theo nhu cầu.'
)
ON CONFLICT DO NOTHING;

INSERT INTO notifications (id, customer_id, type, title, content, read_at, created_at)
VALUES
    ('a3000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'ORDER_COMPLETED', 'Đơn hàng đã hoàn thành', 'Đơn DM-DEMO-0001 đã giao thành công.',
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days'),
    ('a3000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     'PAYMENT_PENDING', 'Đơn hàng đang chờ thu COD', 'Đơn DM-DEMO-0002 đang chờ bàn giao và thu COD.',
     NULL, NOW() - INTERVAL '12 hours')
ON CONFLICT DO NOTHING;

INSERT INTO chat_conversations (
    id, customer_id, assigned_admin_id, status, last_message_at, created_at, updated_at
)
VALUES (
    'a4000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000001', 'OPEN', NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '1 hour', NOW() - INTERVAL '30 minutes'
)
ON CONFLICT DO NOTHING;

INSERT INTO chat_messages (id, conversation_id, sender_id, sender_role, content, read_at, created_at)
VALUES
    ('a4100000-0000-4000-8000-000000000001', 'a4000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000002', 'CUSTOMER', 'Khi nào đơn COD của tôi được giao?',
     NOW() - INTERVAL '45 minutes', NOW() - INTERVAL '1 hour'),
    ('a4100000-0000-4000-8000-000000000002', 'a4000000-0000-4000-8000-000000000001',
     '00000000-0000-4000-8000-000000000001', 'ADMIN', 'Đơn đang chờ bàn giao và dự kiến giao trong 2-3 ngày.',
     NULL, NOW() - INTERVAL '30 minutes')
ON CONFLICT DO NOTHING;

INSERT INTO daily_sales_metrics (
    metric_date, gross_item_sales_vnd, discount_value_vnd, shipping_fee_vnd,
    net_revenue_vnd, order_count, completed_order_count, updated_at
)
VALUES
    (CURRENT_DATE - 8, 15990000, 1500000, 0, 14490000, 1, 1, NOW()),
    (CURRENT_DATE - 1, 24990000, 0, 40000, 25030000, 1, 0, NOW())
ON CONFLICT (metric_date) DO NOTHING;

INSERT INTO daily_product_metrics (
    metric_date, product_id, variant_id, quantity_sold, gross_sales_vnd,
    net_item_sales_vnd, review_count, rating_sum, updated_at
)
VALUES
    (CURRENT_DATE - 8, '40000000-0000-0000-0000-000000000001',
     '50000000-0000-0000-0000-000000000001', 1, 15990000, 14490000, 1, 5, NOW()),
    (CURRENT_DATE - 1, '40000000-0000-0000-0000-000000000002',
     '50000000-0000-0000-0000-000000000003', 1, 24990000, 24990000, 0, 0, NOW())
ON CONFLICT DO NOTHING;
