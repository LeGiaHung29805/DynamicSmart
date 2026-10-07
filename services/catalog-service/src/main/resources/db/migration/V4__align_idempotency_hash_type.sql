ALTER TABLE idempotency_records
    ALTER COLUMN request_hash TYPE VARCHAR(64)
    USING RTRIM(request_hash);
