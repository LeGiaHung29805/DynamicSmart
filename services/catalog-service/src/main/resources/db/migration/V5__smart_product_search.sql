CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION catalog_unaccent(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
STRICT
AS $$
    SELECT public.unaccent('public.unaccent'::regdictionary, input)
$$;

CREATE OR REPLACE FUNCTION catalog_normalize_search_text(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT btrim(regexp_replace(
        lower(catalog_unaccent(coalesce(input, ''))),
        '[^a-z0-9]+',
        ' ',
        'g'
    ))
$$;

CREATE OR REPLACE FUNCTION catalog_compact_search_text(input TEXT)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT replace(catalog_normalize_search_text(input), ' ', '')
$$;

CREATE OR REPLACE FUNCTION catalog_product_search_document(
    product_name TEXT,
    product_slug TEXT,
    short_description TEXT,
    full_description TEXT
)
RETURNS TEXT
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT catalog_normalize_search_text(concat_ws(
        ' ', product_name, product_slug, short_description, full_description
    ))
$$;

DROP INDEX IF EXISTS idx_products_search_document;

CREATE INDEX idx_products_search_document
    ON products
    USING GIN (to_tsvector('simple', catalog_product_search_document(
        name, slug, short_description, description
    )));

CREATE INDEX idx_products_search_name_trgm
    ON products USING GIN (catalog_normalize_search_text(name) gin_trgm_ops);

CREATE INDEX idx_products_search_name_compact_trgm
    ON products USING GIN (catalog_compact_search_text(name) gin_trgm_ops);

CREATE INDEX idx_categories_search_name_trgm
    ON categories USING GIN (catalog_normalize_search_text(name) gin_trgm_ops);

CREATE INDEX idx_categories_search_name_compact_trgm
    ON categories USING GIN (catalog_compact_search_text(name) gin_trgm_ops);

CREATE INDEX idx_product_variants_search_sku_trgm
    ON product_variants USING GIN (catalog_normalize_search_text(sku) gin_trgm_ops);

CREATE INDEX idx_product_variants_search_sku_compact_trgm
    ON product_variants USING GIN (catalog_compact_search_text(sku) gin_trgm_ops);

CREATE INDEX idx_product_variants_search_name_trgm
    ON product_variants USING GIN (catalog_normalize_search_text(name) gin_trgm_ops);

CREATE INDEX idx_product_variants_search_name_compact_trgm
    ON product_variants USING GIN (catalog_compact_search_text(name) gin_trgm_ops);
