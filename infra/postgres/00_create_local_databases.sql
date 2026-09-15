-- Chạy bằng psql với kết nối vào database mặc định "postgres".
-- Script chỉ tạo database chưa tồn tại; tuyệt đối không DROP hay ghi đè dữ liệu.
SELECT format('CREATE DATABASE %I', database_name)
FROM (
    VALUES
        ('identity_db'),
        ('catalog_db'),
        ('cart_db'),
        ('order_db'),
        ('payment_db'),
        ('engagement_db')
) AS required_databases(database_name)
WHERE NOT EXISTS (
    SELECT 1 FROM pg_database WHERE datname = required_databases.database_name
) \gexec
