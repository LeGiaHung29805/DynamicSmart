-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

INSERT INTO categories (id, code, name, slug, description, status, sort_order)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 'SEED-ELECTRONICS', 'Điện tử', 'seed-dien-tu', 'Thiết bị điện tử và phụ kiện', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000002', 'SEED-FASHION', 'Thời trang', 'seed-thoi-trang', 'Quần áo và phụ kiện thời trang', 'ACTIVE', 20),
    ('b1000000-0000-4000-8000-000000000003', 'SEED-HOME', 'Nhà cửa', 'seed-nha-cua', 'Đồ dùng nhà cửa và đời sống', 'ACTIVE', 30),
    ('b1000000-0000-4000-8000-000000000004', 'SEED-BEAUTY', 'Làm đẹp', 'seed-lam-dep', 'Chăm sóc cá nhân và làm đẹp', 'ACTIVE', 40),
    ('b1000000-0000-4000-8000-000000000005', 'SEED-SPORT', 'Thể thao', 'seed-the-thao', 'Dụng cụ thể thao và dã ngoại', 'ACTIVE', 50),
    ('b1000000-0000-4000-8000-000000000006', 'SEED-BOOK', 'Sách & văn phòng', 'seed-sach-van-phong', 'Sách và dụng cụ văn phòng', 'ACTIVE', 60)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    updated_at = NOW();

WITH source AS (
    SELECT i,
           ((i - 1) % 6) + 1 AS category_no,
           (ARRAY['Tai nghe Bluetooth','Áo thun cotton','Đèn bàn LED','Sữa rửa mặt','Bình nước thể thao','Sổ tay cao cấp'])[((i - 1) % 6) + 1] AS base_name
    FROM generate_series(1, 120) AS s(i)
)
INSERT INTO products (
    id, category_id, name, slug, short_description, description, status,
    published_at, is_featured,
    default_weight_grams, default_length_cm, default_width_cm, default_height_cm,
    created_at, updated_at
)
SELECT
    ('b2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b1000000-0000-4000-8000-' || lpad(category_no::text, 12, '0'))::uuid,
    base_name || ' DM-' || lpad(i::text, 3, '0'),
    'seed-san-pham-' || lpad(i::text, 3, '0'),
    format('Sản phẩm mẫu số %s dùng để kiểm thử DynamicMart.', i),
    format('Dữ liệu đầy đủ cho luồng danh mục, tồn kho, giỏ hàng, đặt hàng, thanh toán và đánh giá. Mã sản phẩm %s.', i),
    'ACTIVE', NOW() - make_interval(days => 120 - (i % 90)), i <= 12,
    250 + (i % 8) * 100, 20 + (i % 5), 15 + (i % 4), 5 + (i % 3),
    NOW() - make_interval(days => 120 - (i % 90)), NOW()
FROM source
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    published_at = EXCLUDED.published_at,
    is_featured = EXCLUDED.is_featured,
    updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial
    FROM generate_series(1, 120) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO product_variants (
    id, product_id, sku, name, price_vnd, weight_grams,
    length_cm, width_cm, height_cm, status, sort_order, created_at, updated_at
)
SELECT
    ('b3000000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(p::text, 12, '0'))::uuid,
    'DM-' || lpad(p::text, 3, '0') || '-' || v,
    (ARRAY['Tiêu chuẩn','Nâng cao','Cao cấp'])[v],
    79000 + p * 7000 + (v - 1) * 25000,
    250 + (p % 8) * 100,
    20 + (p % 5), 15 + (p % 4), 5 + (p % 3),
    'ACTIVE', v, NOW() - make_interval(days => 90 - (p % 60)), NOW()
FROM variants
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    price_vnd = EXCLUDED.price_vnd,
    status = 'ACTIVE',
    updated_at = NOW();

INSERT INTO product_images (id, product_id, image_url, alt_text, sort_order, is_primary)
SELECT
    ('b4000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    CASE ((i - 1) % 6) + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=900&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=900&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80'
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=900&q=80'
    END,
    format('Ảnh sản phẩm DynamicMart %s', i), 0, TRUE
FROM generate_series(1, 120) AS s(i)
ON CONFLICT (id) DO UPDATE SET
    image_url = EXCLUDED.image_url,
    alt_text = EXCLUDED.alt_text,
    updated_at = NOW();

-- Every variant has its own primary image. The catalog lookup prefers this image
-- and falls back to the product-level image above only when a variant image is absent.
WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial
    FROM generate_series(1, 120) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO product_images (
    id, product_id, variant_id, image_url, alt_text, sort_order, is_primary
)
SELECT
    ('b5000000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    ('b2000000-0000-4000-8000-' || lpad(p::text, 12, '0'))::uuid,
    ('b3000000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    CASE ((p - 1) % 6) + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&q=80&w=' || (900 + v * 100)
    END,
    format('Ảnh sản phẩm DynamicMart %s - biến thể %s', p, v),
    v, TRUE
FROM variants
ON CONFLICT (id) DO UPDATE SET
    product_id = EXCLUDED.product_id,
    variant_id = EXCLUDED.variant_id,
    image_url = EXCLUDED.image_url,
    alt_text = EXCLUDED.alt_text,
    sort_order = EXCLUDED.sort_order,
    is_primary = TRUE,
    updated_at = NOW();

-- Backfill a product-level image for any other local demo product. This also
-- covers the smaller R__seed_catalog_demo dataset and future local fixtures.
INSERT INTO product_images (
    id, product_id, variant_id, image_url, alt_text, sort_order, is_primary
)
SELECT
    md5('dynamicmart-product-image-' || p.id::text)::uuid,
    p.id,
    NULL,
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80',
    'Ảnh sản phẩm ' || p.name,
    0,
    TRUE
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images image
    WHERE image.product_id = p.id
      AND image.variant_id IS NULL
      AND image.is_primary = TRUE
)
ON CONFLICT (id) DO UPDATE SET
    image_url = EXCLUDED.image_url,
    alt_text = EXCLUDED.alt_text,
    is_primary = TRUE,
    updated_at = NOW();

-- Backfill a primary image for every remaining variant, so purchasable-variant,
-- cart and checkout responses never fall back to a missing image.
INSERT INTO product_images (
    id, product_id, variant_id, image_url, alt_text, sort_order, is_primary
)
SELECT
    md5('dynamicmart-variant-image-' || variant.id::text)::uuid,
    variant.product_id,
    variant.id,
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
    'Ảnh biến thể ' || variant.sku,
    0,
    TRUE
FROM product_variants variant
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images image
    WHERE image.variant_id = variant.id
      AND image.is_primary = TRUE
)
ON CONFLICT (id) DO UPDATE SET
    image_url = EXCLUDED.image_url,
    alt_text = EXCLUDED.alt_text,
    is_primary = TRUE,
    updated_at = NOW();

-- Replace URLs generated by older revisions of this seeder. The frontend only
-- allows images.unsplash.com, so leaving one placehold.co URL would still break
-- the product detail gallery for that product or variant.
UPDATE product_images image
SET image_url = CASE ((substring(product.slug FROM '([0-9]+)$'))::integer - 1) % 6 + 1
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1000&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?auto=format&fit=crop&w=1000&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1000&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=1000&q=80'
        ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1000&q=80'
    END,
    updated_at = NOW()
FROM products product
WHERE image.product_id = product.id
  AND image.image_url LIKE 'https://placehold.co/%'
  AND product.slug ~ '[0-9]+$';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
    updated_at = NOW()
WHERE image_url LIKE 'https://placehold.co/%';

WITH variants AS (
    SELECT ((p - 1) * 3 + v) AS serial, p, v
    FROM generate_series(1, 120) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO inventory_items (variant_id, on_hand_qty, reserved_qty, version)
SELECT
    ('b3000000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    30 + (p * v) % 71,
    CASE WHEN p % 10 = 0 THEN 3 ELSE p % 3 END,
    0
FROM variants
ON CONFLICT (variant_id) DO UPDATE SET
    on_hand_qty = EXCLUDED.on_hand_qty,
    reserved_qty = EXCLUDED.reserved_qty,
    updated_at = NOW();

SELECT 'catalog' AS domain,
       count(*) FILTER (WHERE id::text LIKE 'b2000000-%') AS products,
       (SELECT count(*) FROM product_variants WHERE id::text LIKE 'b3000000-%') AS variants,
       (SELECT count(*) FROM product_images WHERE id::text LIKE 'b4000000-%') AS product_images,
       (SELECT count(*) FROM product_images WHERE id::text LIKE 'b5000000-%') AS variant_images,
       (SELECT sum(on_hand_qty) FROM inventory_items WHERE variant_id::text LIKE 'b3000000-%') AS stock_units
FROM products;
