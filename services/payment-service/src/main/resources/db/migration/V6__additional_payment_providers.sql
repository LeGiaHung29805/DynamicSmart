ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_method_check
    CHECK (method IN ('VNPAY', 'ZALOPAY', 'PAYOS', 'BANK_QR', 'COD'));

ALTER TABLE payment_attempts DROP CONSTRAINT IF EXISTS payment_attempts_provider_check;
ALTER TABLE payment_attempts ADD CONSTRAINT payment_attempts_provider_check
    CHECK (provider IN ('VNPAY', 'ZALOPAY', 'PAYOS', 'BANK_QR', 'COD'));
