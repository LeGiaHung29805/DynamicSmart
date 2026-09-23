ALTER TABLE checkout_session_items
    ADD COLUMN length_cm INTEGER,
    ADD COLUMN width_cm INTEGER,
    ADD COLUMN height_cm INTEGER,
    ADD CONSTRAINT checkout_session_items_length_check CHECK (length_cm IS NULL OR length_cm > 0),
    ADD CONSTRAINT checkout_session_items_width_check CHECK (width_cm IS NULL OR width_cm > 0),
    ADD CONSTRAINT checkout_session_items_height_check CHECK (height_cm IS NULL OR height_cm > 0);

COMMENT ON COLUMN checkout_session_items.length_cm IS
    'Trusted Catalog snapshot. Nullable only for sessions created before migration V3.';
COMMENT ON COLUMN checkout_session_items.width_cm IS
    'Trusted Catalog snapshot. Nullable only for sessions created before migration V3.';
COMMENT ON COLUMN checkout_session_items.height_cm IS
    'Trusted Catalog snapshot. Nullable only for sessions created before migration V3.';
