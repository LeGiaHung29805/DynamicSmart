-- Local-only repeatable demo seed. Flyway loads this file from db/seed/local.
-- UUIDs are shared across services so catalog, cart, order, payment and engagement data align.

-- Keep the storefront taxonomy intentionally shallow: a small set of root
-- categories for discovery and concrete leaf categories for assigning products.
-- The first electronics root and its Phone/Laptop children are created by
-- R__seed_catalog_demo.sql; this seed extends that tree instead of duplicating it.
INSERT INTO categories (id, parent_id, code, name, slug, description, status, sort_order)
VALUES
    ('10000000-0000-0000-0000-000000000001', NULL,
     'ELECTRONICS', 'Điện tử & Công nghệ', 'dien-tu', 'Điện thoại, laptop, âm thanh và phụ kiện công nghệ', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000001', '10000000-0000-0000-0000-000000000001',
     'AUDIO', 'Tai nghe & Âm thanh', 'tai-nghe-am-thanh', 'Tai nghe và thiết bị âm thanh cá nhân', 'ACTIVE', 30),
    ('b1000000-0000-4000-8000-000000000002', NULL,
     'FASHION', 'Thời trang', 'thoi-trang', 'Quần áo và phụ kiện thời trang', 'ACTIVE', 20),
    ('b1100000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000002',
     'T_SHIRT', 'Áo thun', 'ao-thun', 'Áo thun mặc hằng ngày', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000003', NULL,
     'HOME_LIVING', 'Nhà cửa & Đời sống', 'nha-cua-doi-song', 'Đồ dùng giúp không gian sống tiện nghi hơn', 'ACTIVE', 30),
    ('b1100000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000003',
     'LIGHTING', 'Đèn & Chiếu sáng', 'den-chieu-sang', 'Đèn bàn và thiết bị chiếu sáng', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000004', NULL,
     'BEAUTY_PERSONAL_CARE', 'Làm đẹp & Chăm sóc cá nhân', 'lam-dep-cham-soc-ca-nhan', 'Sản phẩm chăm sóc cá nhân và làm đẹp', 'ACTIVE', 40),
    ('b1100000-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000004',
     'FACIAL_CLEANSER', 'Sữa rửa mặt', 'sua-rua-mat', 'Sản phẩm làm sạch và chăm sóc da mặt', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000005', NULL,
     'SPORT_OUTDOOR', 'Thể thao & Dã ngoại', 'the-thao-da-ngoai', 'Dụng cụ thể thao và hoạt động ngoài trời', 'ACTIVE', 50),
    ('b1100000-0000-4000-8000-000000000005', 'b1000000-0000-4000-8000-000000000005',
     'SPORT_BOTTLE', 'Bình nước thể thao', 'binh-nuoc-the-thao', 'Bình nước dùng khi luyện tập và dã ngoại', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000006', NULL,
     'BOOK_STATIONERY', 'Sách & Văn phòng phẩm', 'sach-van-phong-pham', 'Sách, sổ và dụng cụ học tập, văn phòng', 'ACTIVE', 60),
    ('b1100000-0000-4000-8000-000000000006', 'b1000000-0000-4000-8000-000000000006',
     'NOTEBOOK', 'Sổ tay', 'so-tay', 'Sổ ghi chép cho học tập và công việc', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000007', NULL,
     'KITCHEN_DINING', 'Đồ bếp & Gia dụng', 'do-bep-gia-dung', 'Dụng cụ nấu ăn và đồ dùng nhà bếp', 'ACTIVE', 70),
    ('b1100000-0000-4000-8000-000000000007', 'b1000000-0000-4000-8000-000000000007',
     'COOKWARE', 'Dụng cụ nấu ăn', 'dung-cu-nau-an', 'Chảo, hộp đựng và dụng cụ bếp hằng ngày', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000008', NULL,
     'KIDS_TOYS', 'Đồ chơi & Mẹ bé', 'do-choi-me-be', 'Đồ chơi phát triển kỹ năng và sản phẩm cho trẻ', 'ACTIVE', 80),
    ('b1100000-0000-4000-8000-000000000008', 'b1000000-0000-4000-8000-000000000008',
     'EDUCATIONAL_TOYS', 'Đồ chơi giáo dục', 'do-choi-giao-duc', 'Đồ chơi sáng tạo, an toàn cho trẻ nhỏ', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000009', NULL,
     'PET_SUPPLIES', 'Thú cưng', 'thu-cung', 'Đồ dùng và phụ kiện cho thú cưng', 'ACTIVE', 90),
    ('b1100000-0000-4000-8000-000000000009', 'b1000000-0000-4000-8000-000000000009',
     'PET_ACCESSORIES', 'Phụ kiện thú cưng', 'phu-kien-thu-cung', 'Đồ ăn, đồ chơi và phụ kiện cho thú cưng', 'ACTIVE', 10),
    ('b1000000-0000-4000-8000-000000000010', NULL,
     'TRAVEL_ACCESSORIES', 'Du lịch & Phụ kiện', 'du-lich-phu-kien', 'Balo, vali và phụ kiện cho những chuyến đi', 'ACTIVE', 100),
    ('b1100000-0000-4000-8000-000000000010', 'b1000000-0000-4000-8000-000000000010',
     'TRAVEL_BAGS', 'Balo & Vali', 'balo-vali', 'Balo, vali và túi đựng đồ du lịch', 'ACTIVE', 10)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();

WITH source AS (
    SELECT i,
           ((i - 1) % 6) + 1 AS category_no,
           (ARRAY['Tai nghe Sonic Air','Áo thun Everyday Fit','Đèn bàn Focus LED','Sữa rửa mặt Pure Balance','Bình nước Active Flow','Sổ tay Paper Note'])[((i - 1) % 6) + 1] AS base_name,
           (ARRAY['tai-nghe-sonic-air','ao-thun-everyday-fit','den-ban-focus-led','sua-rua-mat-pure-balance','binh-nuoc-active-flow','so-tay-paper-note'])[((i - 1) % 6) + 1] AS base_slug
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
    CASE category_no
        WHEN 1 THEN 'b1000000-0000-4000-8000-000000000001'::uuid
        ELSE ('b1100000-0000-4000-8000-' || lpad(category_no::text, 12, '0'))::uuid
    END,
    base_name || ' DM-' || lpad(i::text, 3, '0'),
    base_slug || '-dm-' || lpad(i::text, 3, '0'),
    format('Sản phẩm mẫu số %s dùng để kiểm thử DynamicMart.', i),
    format('Dữ liệu đầy đủ cho luồng danh mục, tồn kho, giỏ hàng, đặt hàng, thanh toán và đánh giá. Mã sản phẩm %s.', i),
    'ACTIVE', NOW() - make_interval(days => 120 - (i % 90)), i <= 12,
    250 + (i % 8) * 100, 20 + (i % 5), 15 + (i % 4), 5 + (i % 3),
    NOW() - make_interval(days => 120 - (i % 90)), NOW()
FROM source
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
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
    CASE ((p - 1) % 6) + 1
        WHEN 1 THEN (ARRAY['Đen nhám','Trắng ngọc','Xanh navy'])[v]
        WHEN 2 THEN (ARRAY['Size S','Size M','Size L'])[v]
        WHEN 3 THEN (ARRAY['5 W','9 W','12 W'])[v]
        WHEN 4 THEN (ARRAY['100 ml','150 ml','200 ml'])[v]
        WHEN 5 THEN (ARRAY['500 ml','750 ml','1 lít'])[v]
        ELSE (ARRAY['A5 · 100 trang','A5 · 200 trang','A4 · 200 trang'])[v]
    END,
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

-- Rich local storefront details: readable descriptions, specifications and category-specific images.
INSERT INTO attributes (id, code, name, data_type, validation_config, status)
VALUES
    ('b6000000-0000-4000-8000-000000000001', 'SPECIFICATIONS', 'Thông số nổi bật', 'TEXT', NULL, 'ACTIVE'),
    ('b6000000-0000-4000-8000-000000000002', 'MATERIAL', 'Chất liệu', 'TEXT', NULL, 'ACTIVE'),
    ('b6000000-0000-4000-8000-000000000003', 'WARRANTY', 'Bảo hành & đổi trả', 'TEXT', NULL, 'ACTIVE'),
    ('b6000000-0000-4000-8000-000000000004', 'MODEL_OPTION', 'Phiên bản', 'TEXT', NULL, 'ACTIVE')
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code, name = EXCLUDED.name, data_type = EXCLUDED.data_type,
    status = 'ACTIVE', updated_at = NOW();

WITH category_attribute_seed(category_id, attribute_id, sort_order) AS (
    VALUES
        ('b1000000-0000-4000-8000-000000000001'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1000000-0000-4000-8000-000000000001'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1000000-0000-4000-8000-000000000001'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30),
        ('b1100000-0000-4000-8000-000000000002'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1100000-0000-4000-8000-000000000002'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1100000-0000-4000-8000-000000000002'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30),
        ('b1100000-0000-4000-8000-000000000003'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1100000-0000-4000-8000-000000000003'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1100000-0000-4000-8000-000000000003'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30),
        ('b1100000-0000-4000-8000-000000000004'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1100000-0000-4000-8000-000000000004'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1100000-0000-4000-8000-000000000004'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30),
        ('b1100000-0000-4000-8000-000000000005'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1100000-0000-4000-8000-000000000005'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1100000-0000-4000-8000-000000000005'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30),
        ('b1100000-0000-4000-8000-000000000006'::uuid, 'b6000000-0000-4000-8000-000000000001'::uuid, 10),
        ('b1100000-0000-4000-8000-000000000006'::uuid, 'b6000000-0000-4000-8000-000000000002'::uuid, 20),
        ('b1100000-0000-4000-8000-000000000006'::uuid, 'b6000000-0000-4000-8000-000000000003'::uuid, 30)
)
INSERT INTO category_attributes (id, category_id, attribute_id, applies_to, is_required, is_filterable, sort_order)
SELECT md5('dynamicmart-category-attribute-' || category_id::text || '-' || attribute_id::text)::uuid,
       category_id, attribute_id, 'PRODUCT', FALSE, FALSE, sort_order
FROM category_attribute_seed
ON CONFLICT (category_id, attribute_id, applies_to) DO UPDATE SET sort_order = EXCLUDED.sort_order;

-- Every demo product exposes a real selectable variant. This lets the product
-- detail page change both the selected image and its distinct price.
INSERT INTO category_attributes (id, category_id, attribute_id, applies_to, is_required, is_filterable, sort_order)
SELECT
    md5('dynamicmart-variant-option-mapping-' || category.id::text)::uuid,
    category.id,
    'b6000000-0000-4000-8000-000000000004'::uuid,
    'VARIANT', TRUE, TRUE, 100
FROM categories category
WHERE category.code IN ('AUDIO', 'T_SHIRT', 'LIGHTING', 'FACIAL_CLEANSER', 'SPORT_BOTTLE', 'NOTEBOOK')
ON CONFLICT (category_id, attribute_id, applies_to) DO UPDATE SET
    is_required = TRUE, is_filterable = TRUE, sort_order = EXCLUDED.sort_order;

UPDATE products product
SET short_description = CASE ((substring(product.slug FROM '([0-9]+)$'))::integer - 1) % 6 + 1
        WHEN 1 THEN 'Tai nghe không dây gọn nhẹ, âm thanh rõ và dùng thoải mái hằng ngày.'
        WHEN 2 THEN 'Áo thun cotton mềm, phom dễ mặc cho đi học, đi làm và dạo phố.'
        WHEN 3 THEN 'Đèn bàn LED ánh sáng dịu, tiết kiệm điện cho góc học tập và làm việc.'
        WHEN 4 THEN 'Sữa rửa mặt dịu nhẹ, làm sạch hằng ngày mà không gây khô căng.'
        WHEN 5 THEN 'Bình nước bền nhẹ, tiện mang theo khi tập luyện và dã ngoại.'
        ELSE 'Sổ tay giấy dày, bố cục gọn gàng cho ghi chép học tập và công việc.'
    END,
    description = CASE ((substring(product.slug FROM '([0-9]+)$'))::integer - 1) % 6 + 1
        WHEN 1 THEN 'Tai nghe Bluetooth DynamicMart được thiết kế để nghe nhạc, gọi thoại và học trực tuyến. Kết nối ổn định, đệm tai êm và pin đáp ứng nhu cầu dùng trong ngày.'
        WHEN 2 THEN 'Áo thun cotton DynamicMart có bề mặt mềm, thoáng và dễ phối đồ. Đường may được hoàn thiện gọn, phù hợp mặc thường ngày hoặc vận động nhẹ.'
        WHEN 3 THEN 'Đèn bàn LED DynamicMart cho ánh sáng tập trung, hỗ trợ đọc sách và làm việc buổi tối. Thân đèn nhỏ gọn, điều chỉnh linh hoạt theo góc sử dụng.'
        WHEN 4 THEN 'Sữa rửa mặt DynamicMart làm sạch bụi bẩn và dầu thừa với kết cấu dịu nhẹ. Sản phẩm phù hợp quy trình chăm sóc da cơ bản mỗi ngày.'
        WHEN 5 THEN 'Bình nước thể thao DynamicMart có nắp kín, dễ cầm và dễ vệ sinh. Dung tích thực tế được ghi rõ theo từng biến thể để chọn đúng nhu cầu.'
        ELSE 'Sổ tay DynamicMart sử dụng giấy viết mượt, phù hợp ghi chép, lập kế hoạch và phác thảo ý tưởng. Mỗi biến thể ghi rõ khổ giấy và số trang.'
    END,
    updated_at = NOW()
WHERE product.id::text LIKE 'b2000000-%';

WITH source AS (
    SELECT i, ((i - 1) % 6) + 1 AS category_no,
           ('b2000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid AS product_id
    FROM generate_series(1, 120) AS s(i)
), values_to_insert AS (
    SELECT product_id, category_no, 'b6000000-0000-4000-8000-000000000001'::uuid AS attribute_id,
           CASE category_no
               WHEN 1 THEN 'Bluetooth 5.3 · pin đến 30 giờ · micro đàm thoại'
               WHEN 2 THEN 'Vải 180 GSM · phom regular · giặt máy được'
               WHEN 3 THEN 'LED tiết kiệm điện · 3 mức sáng · cổng USB'
               WHEN 4 THEN 'Dung tích theo biến thể · dùng hằng ngày'
               WHEN 5 THEN 'Dung tích theo biến thể · nắp chống tràn'
               ELSE 'Khổ giấy theo biến thể · giấy 100 gsm'
           END AS value
    FROM source
    UNION ALL
    SELECT product_id, category_no, 'b6000000-0000-4000-8000-000000000002'::uuid,
           CASE category_no
               WHEN 1 THEN 'Nhựa ABS và đệm tai protein mềm'
               WHEN 2 THEN 'Cotton pha sợi co giãn nhẹ'
               WHEN 3 THEN 'Hợp kim nhôm và nhựa ABS'
               WHEN 4 THEN 'Công thức làm sạch dịu nhẹ'
               WHEN 5 THEN 'Nhựa Tritan không BPA'
               ELSE 'Giấy kem chống lóa và bìa giấy cứng'
           END
    FROM source
    UNION ALL
    SELECT product_id, category_no, 'b6000000-0000-4000-8000-000000000003'::uuid,
           CASE category_no
               WHEN 1 THEN 'Bảo hành 12 tháng · đổi trả 7 ngày'
               WHEN 2 THEN 'Đổi trả trong 7 ngày nếu còn nguyên nhãn'
               WHEN 3 THEN 'Bảo hành 12 tháng · đổi trả 7 ngày'
               WHEN 4 THEN 'Đổi trả 7 ngày nếu sản phẩm còn nguyên niêm phong'
               WHEN 5 THEN 'Đổi trả 7 ngày nếu chưa sử dụng'
               ELSE 'Đổi trả 7 ngày nếu sản phẩm còn nguyên vẹn'
           END
    FROM source
)
INSERT INTO product_attribute_values (id, product_id, attribute_id, value_json, created_at, updated_at)
SELECT md5('dynamicmart-product-attribute-' || product_id::text || '-' || attribute_id::text)::uuid,
       product_id, attribute_id, to_jsonb(value), NOW(), NOW()
FROM values_to_insert
ON CONFLICT (product_id, attribute_id) DO UPDATE SET value_json = EXCLUDED.value_json, updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial, ((p - 1) % 6) + 1 AS category_no
    FROM generate_series(1, 120) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO variant_attribute_values (id, variant_id, attribute_id, value_json, created_at, updated_at)
SELECT
    md5('dynamicmart-variant-option-' || serial::text)::uuid,
    ('b3000000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    'b6000000-0000-4000-8000-000000000004'::uuid,
    to_jsonb(CASE category_no
        WHEN 1 THEN (ARRAY['Đen nhám · pin 30 giờ','Trắng ngọc · pin 36 giờ','Xanh navy · pin 42 giờ'])[v]
        WHEN 2 THEN (ARRAY['Size S · form regular','Size M · form regular','Size L · form regular'])[v]
        WHEN 3 THEN (ARRAY['5 W · ánh sáng vàng','9 W · 3 mức sáng','12 W · sạc USB-C'])[v]
        WHEN 4 THEN (ARRAY['100 ml · da thường','150 ml · da hỗn hợp','200 ml · dùng gia đình'])[v]
        WHEN 5 THEN (ARRAY['500 ml · tập gym','750 ml · đi làm','1 lít · dã ngoại'])[v]
        ELSE (ARRAY['A5 · 100 trang','A5 · 200 trang','A4 · 200 trang'])[v]
    END),
    NOW(), NOW()
FROM variants
ON CONFLICT (variant_id, attribute_id) DO UPDATE SET value_json = EXCLUDED.value_json, updated_at = NOW();

WITH image_source AS (
    SELECT image.id, category.code, COALESCE(variant.sort_order, 0) AS variant_sort_order,
           product.name AS product_name, image.variant_id
    FROM product_images image
    JOIN products product ON product.id = image.product_id
    JOIN categories category ON category.id = product.category_id
    LEFT JOIN product_variants variant ON variant.id = image.variant_id
    WHERE product.id::text LIKE 'b2000000-%'
       OR product.id IN ('40000000-0000-0000-0000-000000000001'::uuid, '40000000-0000-0000-0000-000000000002'::uuid)
)
UPDATE product_images image
SET image_url = CASE image_source.code
        WHEN 'AUDIO' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'T_SHIRT' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1503341504253-dff4815485f1?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'LIGHTING' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1540932239986-30128078f3c5?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'FACIAL_CLEANSER' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1556229010-6c3f2c9ca5f8?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1612817288484-6f916006741a?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'SPORT_BOTTLE' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1526401485004-2aa7b5c67673?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'NOTEBOOK' THEN CASE image_source.variant_sort_order WHEN 2 THEN 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=84' WHEN 3 THEN 'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=1000&q=84' ELSE 'https://images.unsplash.com/photo-1495446815901-a7297e633e8d?auto=format&fit=crop&w=1000&q=84' END
        WHEN 'PHONE' THEN 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=1000&q=84'
        WHEN 'LAPTOP' THEN 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=1000&q=84'
        ELSE image.image_url
    END,
    alt_text = image_source.product_name || CASE WHEN image_source.variant_id IS NULL THEN '' ELSE ' · ảnh biến thể' END,
    updated_at = NOW()
FROM image_source
WHERE image.id = image_source.id;
-- More varied local storefront inventory: four additional departments, each
-- with ten products, three purchasable variants and distinct variant imagery.
WITH selected_categories AS (
    SELECT id FROM categories
    WHERE code IN ('COOKWARE', 'EDUCATIONAL_TOYS', 'PET_ACCESSORIES', 'TRAVEL_BAGS')
), selected_attributes AS (
    SELECT id, row_number() OVER (ORDER BY id) * 10 AS sort_order FROM attributes
    WHERE id IN (
        'b6000000-0000-4000-8000-000000000001'::uuid,
        'b6000000-0000-4000-8000-000000000002'::uuid,
        'b6000000-0000-4000-8000-000000000003'::uuid
    )
)
INSERT INTO category_attributes (id, category_id, attribute_id, applies_to, is_required, is_filterable, sort_order)
SELECT md5('dynamicmart-extra-product-attribute-' || category.id::text || '-' || attribute.id::text)::uuid,
       category.id, attribute.id, 'PRODUCT', FALSE, FALSE, attribute.sort_order
FROM selected_categories category
CROSS JOIN selected_attributes attribute
ON CONFLICT (category_id, attribute_id, applies_to) DO UPDATE SET
    is_required = FALSE, is_filterable = FALSE, sort_order = EXCLUDED.sort_order;

INSERT INTO category_attributes (id, category_id, attribute_id, applies_to, is_required, is_filterable, sort_order)
SELECT md5('dynamicmart-extra-variant-option-' || category.id::text)::uuid,
       category.id, 'b6000000-0000-4000-8000-000000000004'::uuid, 'VARIANT', TRUE, TRUE, 100
FROM categories category
WHERE category.code IN ('COOKWARE', 'EDUCATIONAL_TOYS', 'PET_ACCESSORIES', 'TRAVEL_BAGS')
ON CONFLICT (category_id, attribute_id, applies_to) DO UPDATE SET
    is_required = TRUE, is_filterable = TRUE, sort_order = EXCLUDED.sort_order;

WITH source AS (
    SELECT i, ((i - 1) % 4) + 1 AS category_no,
           CASE ((i - 1) % 4) + 1
               WHEN 1 THEN (ARRAY['Bộ chảo Ceramic Home','Chảo sâu lòng Pro','Nồi inox Family','Bộ dao bếp Sharp','Thớt tre Eco','Hộp thực phẩm Glass','Bình giữ nhiệt Chef','Khuôn nướng EasyBake','Giá để gia vị','Bộ dụng cụ silicone'])[((i - 1) / 4) + 1]
               WHEN 2 THEN (ARRAY['Bộ xếp hình Sáng tạo','Bảng chữ cái vui học','Bộ thí nghiệm Mini','Đường ray gỗ phiêu lưu','Bộ thẻ Flashcard','Tranh ghép hình 3D','Bộ đất nặn sắc màu','Bảng vẽ tự xóa','Bộ lắp ráp Robot','Bộ câu cá nam châm'])[((i - 1) / 4) + 1]
               WHEN 3 THEN (ARRAY['Bát ăn thú cưng Smart Bowl','Bình nước Pet Go','Dây dắt Urban','Vòng cổ phản quang','Nệm nằm Cozy','Lược chải lông Soft','Đồ chơi dây thừng','Khay vệ sinh sạch','Túi đựng hạt kín mùi','Áo mưa Paw'])[((i - 1) / 4) + 1]
               ELSE (ARRAY['Balo du lịch Trail Pack','Vali Cabin Move','Túi du lịch Weekender','Túi đeo chéo Explorer','Gối cổ Memory','Bộ túi phân loại hành lý','Túi đựng hộ chiếu','Balo chống nước Trek','Túi mỹ phẩm Travel','Dây đai vali Secure'])[((i - 1) / 4) + 1]
           END AS base_name,
           CASE ((i - 1) % 4) + 1
               WHEN 1 THEN (ARRAY['bo-chao-ceramic-home','chao-sau-long-pro','noi-inox-family','bo-dao-bep-sharp','thot-tre-eco','hop-thuc-pham-glass','binh-giu-nhiet-chef','khuon-nuong-easybake','gia-de-gia-vi','bo-dung-cu-silicone'])[((i - 1) / 4) + 1]
               WHEN 2 THEN (ARRAY['bo-xep-hinh-sang-tao','bang-chu-cai-vui-hoc','bo-thi-nghiem-mini','duong-ray-go-phieu-luu','bo-the-flashcard','tranh-ghep-hinh-3d','bo-dat-nan-sac-mau','bang-ve-tu-xoa','bo-lap-rap-robot','bo-cau-ca-nam-cham'])[((i - 1) / 4) + 1]
               WHEN 3 THEN (ARRAY['bat-an-thu-cung-smart-bowl','binh-nuoc-pet-go','day-dat-urban','vong-co-phan-quang','nem-nam-cozy','luoc-chai-long-soft','do-choi-day-thung','khay-ve-sinh-sach','tui-dung-hat-kin-mui','ao-mua-paw'])[((i - 1) / 4) + 1]
               ELSE (ARRAY['balo-du-lich-trail-pack','vali-cabin-move','tui-du-lich-weekender','tui-deo-cheo-explorer','goi-co-memory','bo-tui-phan-loai-hanh-ly','tui-dung-ho-chieu','balo-chong-nuoc-trek','tui-my-pham-travel','day-dai-vali-secure'])[((i - 1) / 4) + 1]
           END AS base_slug
    FROM generate_series(1, 40) AS s(i)
)
INSERT INTO products (
    id, category_id, name, slug, short_description, description, status,
    published_at, is_featured,
    default_weight_grams, default_length_cm, default_width_cm, default_height_cm,
    created_at, updated_at
)
SELECT
    ('b7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b1100000-0000-4000-8000-' || lpad((category_no + 6)::text, 12, '0'))::uuid,
    base_name || ' DM-' || lpad(i::text, 3, '0'),
    base_slug || '-dm-' || lpad(i::text, 3, '0'),
    CASE category_no
        WHEN 1 THEN 'Bộ dụng cụ bếp bền đẹp cho những bữa ăn hằng ngày.'
        WHEN 2 THEN 'Đồ chơi an toàn, hỗ trợ trẻ khám phá và sáng tạo.'
        WHEN 3 THEN 'Phụ kiện tiện dụng giúp thú cưng ăn uống thoải mái.'
        ELSE 'Balo gọn nhẹ, sắp xếp hành lý tiện lợi cho mọi chuyến đi.'
    END,
    CASE category_no
        WHEN 1 THEN 'Sản phẩm đồ bếp DynamicMart được chọn theo nhu cầu nấu nướng hằng ngày. Mỗi biến thể ghi rõ kích thước và công năng để dễ so sánh trước khi mua.'
        WHEN 2 THEN 'Sản phẩm đồ chơi giáo dục DynamicMart ưu tiên chất liệu an toàn và cách chơi trực quan. Mỗi phiên bản phù hợp một độ tuổi và mức độ thử thách khác nhau.'
        WHEN 3 THEN 'Phụ kiện thú cưng DynamicMart có thiết kế dễ vệ sinh và phù hợp sinh hoạt hằng ngày. Hãy chọn dung tích tương ứng với kích cỡ thú cưng.'
        ELSE 'Balo du lịch DynamicMart có các ngăn sắp xếp rõ ràng, phù hợp đi làm, cuối tuần hoặc hành lý xách tay. Mỗi biến thể có dung tích riêng.'
    END,
    'ACTIVE', NOW() - make_interval(days => 40 - i), (i % 5 = 0),
    300 + category_no * 180, 22 + category_no * 3, 16 + category_no * 2, 8 + category_no,
    NOW() - make_interval(days => 40 - i), NOW()
FROM source
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    slug = EXCLUDED.slug,
    short_description = EXCLUDED.short_description,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    published_at = EXCLUDED.published_at,
    is_featured = EXCLUDED.is_featured,
    updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial, ((p - 1) % 4) + 1 AS category_no
    FROM generate_series(1, 40) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO product_variants (
    id, product_id, sku, name, price_vnd, weight_grams,
    length_cm, width_cm, height_cm, status, sort_order, created_at, updated_at
)
SELECT
    ('b7100000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    ('b7000000-0000-4000-8000-' || lpad(p::text, 12, '0'))::uuid,
    'DMX-' || lpad(p::text, 3, '0') || '-' || v,
    CASE category_no
        WHEN 1 THEN (ARRAY['20 cm · dùng bếp từ','24 cm · dùng hằng ngày','28 cm · gia đình'])[v]
        WHEN 2 THEN (ARRAY['48 miếng · 3+','96 miếng · 5+','160 miếng · 7+'])[v]
        WHEN 3 THEN (ARRAY['500 ml · mèo nhỏ','900 ml · chó nhỏ','1,5 lít · thú cưng lớn'])[v]
        ELSE (ARRAY['20 lít · hằng ngày','32 lít · cuối tuần','45 lít · hành lý xách tay'])[v]
    END,
    129000 + category_no * 40000 + p * 6000 + (v - 1) * 50000,
    300 + category_no * 180,
    22 + category_no * 3, 16 + category_no * 2, 8 + category_no,
    'ACTIVE', v, NOW() - make_interval(days => 40 - p), NOW()
FROM variants
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    price_vnd = EXCLUDED.price_vnd,
    status = 'ACTIVE',
    updated_at = NOW();

WITH source AS (
    SELECT i, ((i - 1) % 4) + 1 AS category_no
    FROM generate_series(1, 40) AS s(i)
)
INSERT INTO product_images (id, product_id, image_url, alt_text, sort_order, is_primary)
SELECT
    ('b7200000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    ('b7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid,
    CASE category_no
        WHEN 1 THEN (ARRAY[
            'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=1000&q=84'
        ])[((i - 1) % 3) + 1]
        WHEN 2 THEN (ARRAY[
            'https://images.unsplash.com/photo-1558060370-d644479cb6f7?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1599623560574-39d485900c95?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1559454403-b8fb88521f11?auto=format&fit=crop&w=1000&q=84'
        ])[((i - 1) % 3) + 1]
        WHEN 3 THEN (ARRAY[
            'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1587300003388-59208cc962cb?auto=format&fit=crop&w=1000&q=84'
        ])[((i - 1) % 3) + 1]
        ELSE (ARRAY[
            'https://images.unsplash.com/photo-1553531889-56e6fb9a54b5?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1486911278844-a81c5267e227?auto=format&fit=crop&w=1000&q=84'
        ])[((i - 1) % 3) + 1]
    END,
    'Ảnh ' || (ARRAY['đồ bếp DynamicMart','đồ chơi DynamicMart','phụ kiện thú cưng DynamicMart','balo du lịch DynamicMart'])[category_no] || ' ' || i,
    0, TRUE
FROM source
ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, alt_text = EXCLUDED.alt_text, updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial, ((p - 1) % 4) + 1 AS category_no
    FROM generate_series(1, 40) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO product_images (id, product_id, variant_id, image_url, alt_text, sort_order, is_primary)
SELECT
    ('b7300000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    ('b7000000-0000-4000-8000-' || lpad(p::text, 12, '0'))::uuid,
    ('b7100000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
    CASE category_no
        WHEN 1 THEN (ARRAY[
            'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1503602642458-232111445657?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1556228453-efd6c1ff04f6?auto=format&fit=crop&w=1000&q=84'
        ])[v]
        WHEN 2 THEN (ARRAY[
            'https://images.unsplash.com/photo-1558060370-d644479cb6f7?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1599623560574-39d485900c95?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1559454403-b8fb88521f11?auto=format&fit=crop&w=1000&q=84'
        ])[v]
        WHEN 3 THEN (ARRAY[
            'https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1587300003388-59208cc962cb?auto=format&fit=crop&w=1000&q=84'
        ])[v]
        ELSE (ARRAY[
            'https://images.unsplash.com/photo-1553531889-56e6fb9a54b5?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1000&q=84',
            'https://images.unsplash.com/photo-1486911278844-a81c5267e227?auto=format&fit=crop&w=1000&q=84'
        ])[v]
    END,
    'Ảnh biến thể DMX-' || lpad(p::text, 3, '0') || '-' || v,
    v, TRUE
FROM variants
ON CONFLICT (id) DO UPDATE SET
    product_id = EXCLUDED.product_id, variant_id = EXCLUDED.variant_id,
    image_url = EXCLUDED.image_url, alt_text = EXCLUDED.alt_text,
    sort_order = EXCLUDED.sort_order, is_primary = TRUE, updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial
    FROM generate_series(1, 40) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO inventory_items (variant_id, on_hand_qty, reserved_qty, version)
SELECT ('b7100000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
       25 + (serial % 70), 0, 0
FROM variants
ON CONFLICT (variant_id) DO UPDATE SET on_hand_qty = EXCLUDED.on_hand_qty, updated_at = NOW();

WITH source AS (
    SELECT i, ((i - 1) % 4) + 1 AS category_no,
           ('b7000000-0000-4000-8000-' || lpad(i::text, 12, '0'))::uuid AS product_id
    FROM generate_series(1, 40) AS s(i)
), values_to_insert AS (
    SELECT product_id, 'b6000000-0000-4000-8000-000000000001'::uuid AS attribute_id,
           CASE category_no WHEN 1 THEN 'Lớp phủ ceramic · dùng được bếp từ' WHEN 2 THEN 'Chi tiết lớn · an toàn theo độ tuổi' WHEN 3 THEN 'Dung tích theo biến thể · dễ vệ sinh' ELSE 'Dung tích theo biến thể · nhiều ngăn tiện dụng' END AS value FROM source
    UNION ALL SELECT product_id, 'b6000000-0000-4000-8000-000000000002'::uuid,
           CASE category_no WHEN 1 THEN 'Nhôm phủ ceramic và tay cầm cách nhiệt' WHEN 2 THEN 'Nhựa ABS bo tròn, không cạnh sắc' WHEN 3 THEN 'Nhựa PP thực phẩm, chống trượt' ELSE 'Vải polyester chống bám nước' END FROM source
    UNION ALL SELECT product_id, 'b6000000-0000-4000-8000-000000000003'::uuid,
           'Đổi trả trong 7 ngày nếu sản phẩm còn nguyên vẹn' FROM source
)
INSERT INTO product_attribute_values (id, product_id, attribute_id, value_json, created_at, updated_at)
SELECT md5('dynamicmart-extra-product-attribute-' || product_id::text || '-' || attribute_id::text)::uuid,
       product_id, attribute_id, to_jsonb(value), NOW(), NOW()
FROM values_to_insert
ON CONFLICT (product_id, attribute_id) DO UPDATE SET value_json = EXCLUDED.value_json, updated_at = NOW();

WITH variants AS (
    SELECT p, v, ((p - 1) * 3 + v) AS serial, ((p - 1) % 4) + 1 AS category_no
    FROM generate_series(1, 40) AS products(p)
    CROSS JOIN generate_series(1, 3) AS variant(v)
)
INSERT INTO variant_attribute_values (id, variant_id, attribute_id, value_json, created_at, updated_at)
SELECT md5('dynamicmart-extra-variant-option-' || serial::text)::uuid,
       ('b7100000-0000-4000-8000-' || lpad(serial::text, 12, '0'))::uuid,
       'b6000000-0000-4000-8000-000000000004'::uuid,
       to_jsonb(CASE category_no
           WHEN 1 THEN (ARRAY['20 cm · dùng bếp từ','24 cm · dùng hằng ngày','28 cm · gia đình'])[v]
           WHEN 2 THEN (ARRAY['48 miếng · 3+','96 miếng · 5+','160 miếng · 7+'])[v]
           WHEN 3 THEN (ARRAY['500 ml · mèo nhỏ','900 ml · chó nhỏ','1,5 lít · thú cưng lớn'])[v]
           ELSE (ARRAY['20 lít · hằng ngày','32 lít · cuối tuần','45 lít · hành lý xách tay'])[v]
       END), NOW(), NOW()
FROM variants
ON CONFLICT (variant_id, attribute_id) DO UPDATE SET value_json = EXCLUDED.value_json, updated_at = NOW();