ALTER TABLE chat_messages
    ADD COLUMN idempotency_key UUID;

CREATE UNIQUE INDEX idx_chat_messages_idempotency_key
    ON chat_messages (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
