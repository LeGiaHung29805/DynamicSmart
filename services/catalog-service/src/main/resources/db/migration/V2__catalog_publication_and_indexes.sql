ALTER TABLE products
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT ck_products_active_publication
        CHECK (status <> 'ACTIVE' OR published_at IS NOT NULL);

CREATE INDEX idx_products_public_listing
    ON products (status, published_at DESC, created_at DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_products_featured
    ON products (is_featured, published_at DESC)
    WHERE status = 'ACTIVE' AND is_featured = TRUE;

CREATE INDEX idx_products_search_document
    ON products
    USING GIN (to_tsvector('simple',
        coalesce(name, '') || ' ' ||
        coalesce(slug, '') || ' ' ||
        coalesce(short_description, '')));

CREATE INDEX idx_attribute_options_attribute_status
    ON attribute_options (attribute_id, status, sort_order);

CREATE INDEX idx_category_attributes_category
    ON category_attributes (category_id, applies_to, sort_order);

CREATE INDEX idx_category_attributes_attribute
    ON category_attributes (attribute_id);

CREATE INDEX idx_product_images_product_sort
    ON product_images (product_id, sort_order);

CREATE INDEX idx_product_attribute_values_attribute_value
    ON product_attribute_values USING GIN (value_json);

CREATE INDEX idx_variant_attribute_values_attribute_value
    ON variant_attribute_values USING GIN (value_json);

CREATE INDEX idx_inventory_reservation_items_variant
    ON inventory_reservation_items (variant_id);

CREATE INDEX idx_inventory_adjustments_variant_created
    ON inventory_adjustments (variant_id, created_at DESC);
