ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_check;

ALTER TABLE orders ADD CONSTRAINT orders_payment_method_check
    CHECK (payment_method IN ('VNPAY', 'ZALOPAY', 'PAYOS', 'BANK_QR', 'COD', 'FREE'));

ALTER TABLE orders ADD CONSTRAINT orders_payment_timing_method_check
    CHECK (
        (payment_timing = 'PREPAID' AND payment_method IN ('VNPAY', 'ZALOPAY', 'PAYOS', 'BANK_QR'))
        OR (payment_timing = 'POSTPAID' AND payment_method IN ('VNPAY', 'ZALOPAY', 'PAYOS', 'BANK_QR', 'COD'))
        OR (payment_timing = 'NOT_REQUIRED' AND payment_method = 'FREE')
    );
