INSERT INTO notifications (id, customer_id, type, title, content, read_at, created_at)
VALUES
    ('a3000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'ORDER_COMPLETED', 'Đơn hàng đã hoàn tất',
     'Đơn hàng #DM-DEMO-0001 của bạn đã hoàn tất. Bạn có thể đánh giá sản phẩm đã mua.', NULL, NOW() - INTERVAL '4 hours'),
    ('a3000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000002',
     'PAYMENT_SUCCEEDED', 'Thanh toán thành công',
     'Hệ thống đã ghi nhận thanh toán 15,990,000 đ cho đơn hàng của bạn.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('a3000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000002',
     'ORDER_COMPLETED', 'Đơn hàng đã hoàn tất',
     'Đơn hàng #DM-DEMO-0002 của bạn đã hoàn tất. Bạn có thể đánh giá sản phẩm đã mua.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days'),
    ('a3000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000002',
     'ORDER_CANCELLED', 'Đơn hàng đã bị hủy',
     'Đơn hàng #DM-DEMO-0000 đã bị hủy. Lý do: Khách hàng đổi ý muốn đổi phiên bản khác.', NULL, NOW() - INTERVAL '5 days')
ON CONFLICT (id) DO NOTHING;

