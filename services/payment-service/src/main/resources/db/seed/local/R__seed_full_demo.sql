-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.
-- GHN provinces/districts/wards are synchronized from the provider API at runtime.

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           ((i - 1) % 12) + 1 AS customer_no,
           CASE (i % 8)
               WHEN 1 THEN 'PENDING_PAYMENT' WHEN 2 THEN 'CONFIRMED'
               WHEN 3 THEN 'PACKING' WHEN 4 THEN 'SHIPPING'
               WHEN 5 THEN 'HANDOVER_PENDING' WHEN 6 THEN 'DELIVERED'
               WHEN 7 THEN 'COMPLETED' ELSE 'CANCELLED'
           END AS order_status,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO payments (
    id, order_id, customer_id, timing, method, amount_vnd, status,
    expires_at, paid_at, failed_at, provider_transaction_ref,
    correlation_id, created_at, updated_at
)
SELECT
    ('e1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('d4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad(customer_no::text, 12, '0'))::uuid,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'PREPAID' ELSE 'POSTPAID' END,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'VNPAY' ELSE 'COD' END,
    (79000 + product_no * 7000) * quantity + 30000,
    CASE
        WHEN order_status = 'PENDING_PAYMENT' THEN 'PENDING'
        WHEN order_status = 'CANCELLED' THEN 'FAILED'
        WHEN i % 2 = 0 OR order_status IN ('DELIVERED', 'COMPLETED') THEN 'PAID'
        ELSE 'PENDING'
    END,
    CASE WHEN order_status = 'PENDING_PAYMENT' THEN NOW() + INTERVAL '30 minutes' END,
    CASE WHEN order_status NOT IN ('PENDING_PAYMENT', 'CANCELLED') AND (i % 2 = 0 OR order_status IN ('DELIVERED', 'COMPLETED')) THEN NOW() - make_interval(days => 48 - i) END,
    CASE WHEN order_status = 'CANCELLED' THEN NOW() - make_interval(days => 48 - i) END,
    CASE WHEN order_status NOT IN ('PENDING_PAYMENT', 'CANCELLED') AND (i % 2 = 0 OR order_status IN ('DELIVERED', 'COMPLETED')) THEN 'SEED-TXN-' || lpad(i::text, 8, '0') END,
    ('da000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 46 - i)
FROM data
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    paid_at = EXCLUDED.paid_at,
    failed_at = EXCLUDED.failed_at,
    provider_transaction_ref = EXCLUDED.provider_transaction_ref,
    updated_at = NOW();

WITH data AS (
    SELECT i,
           (((i * 2) - 1) % 120) + 1 AS product_no,
           CASE (i % 8)
               WHEN 1 THEN 'PENDING_PAYMENT' WHEN 2 THEN 'CONFIRMED'
               WHEN 3 THEN 'PACKING' WHEN 4 THEN 'SHIPPING'
               WHEN 5 THEN 'HANDOVER_PENDING' WHEN 6 THEN 'DELIVERED'
               WHEN 7 THEN 'COMPLETED' ELSE 'CANCELLED'
           END AS order_status,
           1 + (i % 3) AS quantity
    FROM generate_series(1, 48) AS s(i)
)
INSERT INTO payment_attempts (
    id, payment_id, attempt_no, provider, provider_reference,
    amount_vnd, request_payload, response_payload, status,
    redirect_url, expires_at, created_at, updated_at
)
SELECT
    ('e2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('e1000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    1,
    CASE WHEN order_status IN ('PENDING_PAYMENT', 'CANCELLED') OR i % 2 = 0 THEN 'VNPAY' ELSE 'COD' END,
    'SEED-ATTEMPT-' || lpad(i::text, 8, '0'),
    (79000 + product_no * 7000) * quantity + 30000,
    jsonb_build_object('seed', true, 'orderNumber', 'SEED' || lpad(i::text, 8, '0')),
    jsonb_build_object('result', lower(order_status)),
    CASE
        WHEN order_status = 'PENDING_PAYMENT' THEN 'REDIRECTED'
        WHEN order_status = 'CANCELLED' THEN 'FAILED'
        WHEN i % 2 = 0 OR order_status IN ('DELIVERED', 'COMPLETED') THEN 'SUCCEEDED'
        ELSE 'CREATED'
    END,
    CASE WHEN order_status = 'PENDING_PAYMENT' THEN format('https://sandbox.vnpayment.vn/seed/%s', i) END,
    CASE WHEN order_status = 'PENDING_PAYMENT' THEN NOW() + INTERVAL '30 minutes' END,
    NOW() - make_interval(days => 49 - i), NOW() - make_interval(days => 46 - i)
FROM data
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, response_payload = EXCLUDED.response_payload, updated_at = NOW();

INSERT INTO shipping_quotes (
    id, customer_id, province_id, ward_id, items_fingerprint,
    request_fingerprint, fee_vnd, shipping_discount_vnd,
    service_id, service_name, eta_text, expires_at, used_at, created_at
)
SELECT
    ('e3000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('a1000000-0000-4000-8000-' || lpad((((i - 1) % 12) + 1)::text, 12, '0'))::uuid,
    201, 11007,
    repeat(substr(md5('seed-items-' || i), 1, 32), 2),
    repeat(substr(md5('seed-request-' || i), 1, 32), 2),
    30000, 0, 53321, 'GHN Tiêu chuẩn', 'Giao trong 2-3 ngày',
    NOW() + INTERVAL '30 days', NOW() - make_interval(days => 49 - i),
    NOW() - make_interval(days => 49 - i)
FROM generate_series(1, 48) AS s(i)
ON CONFLICT (id) DO UPDATE SET expires_at = EXCLUDED.expires_at;

SELECT 'payment' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'e1000000-%') AS payments,
       (SELECT count(*) FROM payment_attempts WHERE id::text LIKE 'e2000000-%') AS attempts,
       jsonb_object_agg(status, status_count ORDER BY status) AS statuses
FROM (
    SELECT id, status, count(*) OVER (PARTITION BY status) AS status_count
    FROM payments WHERE id::text LIKE 'e1000000-%'
) summary;
