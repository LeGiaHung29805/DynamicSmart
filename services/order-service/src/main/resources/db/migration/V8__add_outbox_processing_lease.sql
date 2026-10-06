ALTER TABLE outbox_events
    ADD COLUMN processing_owner UUID,
    ADD COLUMN processing_lease_until TIMESTAMPTZ;

CREATE INDEX idx_outbox_events_publish_claim
    ON outbox_events (status, available_at, processing_lease_until, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');
