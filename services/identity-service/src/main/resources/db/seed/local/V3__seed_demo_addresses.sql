-- Local demo addresses for the seeded customers.
INSERT INTO addresses (
    id, user_id, recipient_name, phone, address_line,
    province_id, ward_id, province_name, ward_name,
    location_validated_at, is_default, status
)
VALUES
    ('01000000-0000-4000-8000-000000000001', '00000000-0000-4000-8000-000000000002',
     'Khách hàng Demo', '0900000002', '41A Đường Phú Diễn, Phường Phú Diễn',
     201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW(), TRUE, 'ACTIVE'),
    ('01000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000003',
     'Khách hàng Demo 2', '0900000003', '12 Hồ Tùng Mậu, Phường Phú Diễn',
     201, 11007, 'Hà Nội', 'Phường Phú Diễn', NOW(), TRUE, 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
