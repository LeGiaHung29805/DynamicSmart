-- Chỉ được Flyway chạy khi SPRING_PROFILES_ACTIVE=local.
-- Mật khẩu chung: Password@123. Đổi hoặc xóa các tài khoản này trước khi demo thật.
INSERT INTO users (id, email, email_normalized, password_hash, full_name, phone, role, status, email_verified_at)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'admin@dynamicmart.local', 'admin@dynamicmart.local', '$2a$10$tBqz77DaSxqW4Idt2NIdS.7tUnuf.ITT0e2CA.rWK88aSI2WycjVK', 'Quản trị viên DynamicMart', '0900000001', 'ADMIN', 'ACTIVE', NOW()),
    ('00000000-0000-4000-8000-000000000002', 'customer@dynamicmart.local', 'customer@dynamicmart.local', '$2a$10$tBqz77DaSxqW4Idt2NIdS.7tUnuf.ITT0e2CA.rWK88aSI2WycjVK', 'Khách hàng Demo', '0900000002', 'CUSTOMER', 'ACTIVE', NOW()),
    ('00000000-0000-4000-8000-000000000003', 'customer2@dynamicmart.local', 'customer2@dynamicmart.local', '$2a$10$tBqz77DaSxqW4Idt2NIdS.7tUnuf.ITT0e2CA.rWK88aSI2WycjVK', 'Khách hàng Demo 2', '0900000003', 'CUSTOMER', 'ACTIVE', NOW());
