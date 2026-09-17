package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.LocationSyncResponse;
import com.dynamicmart.payment_service.entity.GhnLocationProvince;
import com.dynamicmart.payment_service.entity.GhnLocationWard;
import com.dynamicmart.payment_service.repository.GhnLocationProvinceRepository;
import com.dynamicmart.payment_service.repository.GhnLocationWardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GhnLocationSyncService {
    private final GhnClient ghn;
    private final GhnLocationProvinceRepository provinces;
    private final GhnLocationWardRepository wards;

    public GhnLocationSyncService(GhnClient ghn, GhnLocationProvinceRepository provinces, GhnLocationWardRepository wards) {
        this.ghn = ghn; this.provinces = provinces; this.wards = wards;
    }

    /** Upserts the current GHN catalog. Existing records are retained for order history and can be deactivated separately. */
    @Transactional
    public LocationSyncResponse sync() {
        int provinceCount = 0; int wardCount = 0; Instant now = Instant.now();
        for (JsonNode source : data(ghn.provinces())) {
            int provinceId = integer(source, "ProvinceID", "province_id", "id"); String provinceName = text(source, "ProvinceName", "province_name", "name");
            if (provinceId <= 0 || provinceName.isBlank()) continue;
            GhnLocationProvince province = provinces.findById(provinceId).orElseGet(GhnLocationProvince::new);
            province.setId(provinceId); province.setName(provinceName); province.setNameNormalized(normalize(provinceName)); province.setGhnUpdatedAt(now); province.setSyncedAt(now); province.setActive(true); provinces.save(province); provinceCount++;
            for (JsonNode wardSource : data(ghn.wards(provinceId))) {
                int wardId = integer(wardSource, "WardCode", "ward_id", "id"); String wardName = text(wardSource, "WardName", "ward_name", "name");
                if (wardId <= 0 || wardName.isBlank()) continue;
                GhnLocationWard ward = wards.findById(wardId).orElseGet(GhnLocationWard::new);
                ward.setId(wardId); ward.setProvinceId(provinceId); ward.setName(wardName); ward.setNameNormalized(normalize(wardName)); ward.setGhnUpdatedAt(now); ward.setSyncedAt(now); ward.setActive(true); wards.save(ward); wardCount++;
            }
        }
        return new LocationSyncResponse(provinceCount, wardCount);
    }

    private Iterable<JsonNode> data(JsonNode response) { JsonNode data = response == null ? null : response.path("data"); return data != null && data.isArray() ? data : java.util.List.of(); }
    private int integer(JsonNode source, String... fields) { for (String field : fields) { String value = source.path(field).asText(""); try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { } } return -1; }
    private String text(JsonNode source, String... fields) { for (String field : fields) { String value = source.path(field).asText("").trim(); if (!value.isBlank()) return value; } return ""; }
    private String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
}
