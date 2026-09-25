ALTER TABLE payments ADD COLUMN correlation_id UUID;
UPDATE payments SET correlation_id = order_id WHERE correlation_id IS NULL;
ALTER TABLE payments ALTER COLUMN correlation_id SET NOT NULL;

ALTER TABLE shipping_quotes DROP CONSTRAINT IF EXISTS shipping_quotes_request_fingerprint_key;
CREATE INDEX IF NOT EXISTS idx_shipping_quotes_fingerprint_customer
    ON shipping_quotes (request_fingerprint, customer_id, expires_at);
