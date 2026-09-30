CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_provider_transaction_ref
    ON payments (provider_transaction_ref)
    WHERE provider_transaction_ref IS NOT NULL;
