CREATE TABLE ghn_location_districts (
    id INTEGER PRIMARY KEY,
    province_id INTEGER NOT NULL REFERENCES ghn_location_provinces(id),
    name VARCHAR(150) NOT NULL,
    name_normalized VARCHAR(150) NOT NULL,
    ghn_updated_at TIMESTAMPTZ,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_ghn_districts_province_active_name ON ghn_location_districts (province_id, is_active, name_normalized);

ALTER TABLE ghn_location_wards ADD COLUMN district_id INTEGER REFERENCES ghn_location_districts(id);
ALTER TABLE ghn_location_wards ADD COLUMN ghn_ward_code VARCHAR(30);
UPDATE ghn_location_wards SET ghn_ward_code = id::text WHERE ghn_ward_code IS NULL;
CREATE UNIQUE INDEX uq_ghn_wards_district_code ON ghn_location_wards (district_id, ghn_ward_code) WHERE district_id IS NOT NULL;
CREATE INDEX idx_ghn_wards_district_active_name ON ghn_location_wards (district_id, is_active, name_normalized);

ALTER TABLE shipping_quotes ADD COLUMN district_id INTEGER REFERENCES ghn_location_districts(id);
