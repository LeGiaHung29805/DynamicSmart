-- GHN administrative locations are synchronized from the provider API at runtime.

INSERT INTO payments (
    id, order_id, customer_id, timing, method, amount_vnd, status,
    paid_at, provider_transaction_ref, correlation_id, created_at, updated_at
)
VALUES (
    '90000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
    '00000000-0000-4000-8000-000000000002', 'PREPAID', 'VNPAY', 14490000, 'PAID',
    NOW() - INTERVAL '8 days', 'DEMO-VNPAY-0001', '89000000-0000-4000-8000-000000000001',
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'
)
ON CONFLICT DO NOTHING;

INSERT INTO payments (
    id, order_id, customer_id, timing, method, amount_vnd, status,
    correlation_id, created_at, updated_at
)
VALUES (
    '90000000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002',
    '00000000-0000-4000-8000-000000000002', 'POSTPAID', 'COD', 25030000, 'PENDING',
    '89000000-0000-4000-8000-000000000002', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
)
ON CONFLICT DO NOTHING;

INSERT INTO payment_attempts (
    id, payment_id, attempt_no, provider, provider_reference, amount_vnd,
    request_payload, response_payload, status, created_at, updated_at
)
VALUES (
    '90100000-0000-4000-8000-000000000001', '90000000-0000-4000-8000-000000000001',
    1, 'VNPAY', 'DEMO-VNPAY-0001', 14490000,
    '{"demo":true}'::jsonb, '{"responseCode":"00"}'::jsonb, 'SUCCEEDED',
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'
)
ON CONFLICT DO NOTHING;
