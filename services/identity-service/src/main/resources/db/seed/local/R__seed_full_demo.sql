-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

INSERT INTO users (
    id, email, email_normalized, password_hash, full_name, phone,
    role, status, email_verified_at, created_at, updated_at
)
SELECT
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    'seed.customer' || lpad(i::text, 3, '0') || '@dynamicmart.local',
    'seed.customer' || lpad(i::text, 3, '0') || '@dynamicmart.local',
    '$2a$10$tBqz77DaSxqW4Idt2NIdS.7tUnuf.ITT0e2CA.rWK88aSI2WycjVK',
    format('Khách hàng Seed %s', i),
    '091' || lpad(i::text, 7, '0'),
    'CUSTOMER', 'ACTIVE', NOW(),
    NOW() - make_interval(days => 60 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    status = 'ACTIVE',
    updated_at = NOW();

INSERT INTO addresses (
    id, user_id, recipient_name, phone, address_line,
    province_id, ward_id, province_name, ward_name,
    location_validated_at, is_default, status, created_at, updated_at
)
SELECT
    ('a2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    format('Khách hàng Seed %s', i),
    '091' || lpad(i::text, 7, '0'),
    format('%s Nguyễn Trãi, Thanh Xuân', 10 + i),
    201, 11007, 'Hà Nội', 'Phường Phú Diễn',
    NOW(), TRUE, 'ACTIVE', NOW() - make_interval(days => 50 - i), NOW()
FROM generate_series(1, 12) AS s(i)
ON CONFLICT (id) DO UPDATE SET
    recipient_name = EXCLUDED.recipient_name,
    phone = EXCLUDED.phone,
    address_line = EXCLUDED.address_line,
    location_validated_at = NOW(),
    status = 'ACTIVE',
    updated_at = NOW();

SELECT 'identity' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'a1000000-%') AS customers,
       (SELECT count(*) FROM addresses WHERE id::text LIKE 'a2000000-%') AS addresses
FROM users;
