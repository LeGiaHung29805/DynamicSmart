INSERT INTO notifications (id, customer_id, type, title, content, read_at, created_at)
VALUES
    ('a3000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'ORDER_COMPLETED', 'Đơn hàng đã hoàn tất',
     'Đơn hàng mẫu đã hoàn tất. Bạn có thể đánh giá sản phẩm đã mua.', NULL, NOW() - INTERVAL '1 day'),
    ('a3000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     'PAYMENT_SUCCEEDED', 'Thanh toán thành công',
     'Hệ thống đã ghi nhận thanh toán cho đơn hàng mẫu.', NOW(), NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;
