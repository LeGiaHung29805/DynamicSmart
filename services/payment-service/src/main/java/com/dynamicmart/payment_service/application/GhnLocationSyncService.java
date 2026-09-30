package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.LocationSyncResponse;
import tools.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class GhnLocationSyncService {
    private final GhnClient ghn;
    private final GhnLocationCatalogWriter writer;

    public GhnLocationSyncService(GhnClient ghn, GhnLocationCatalogWriter writer) {
        this.ghn = ghn; this.writer = writer;
    }

    /** Fetches the complete provider catalog before opening the local database transaction. */
    public LocationSyncResponse sync() {
        List<ProvinceData> provinces = new ArrayList<>(); List<DistrictData> districts = new ArrayList<>(); List<WardData> wards = new ArrayList<>();
        for (JsonNode source : data(ghn.provinces())) {
            int provinceId = integer(source, "ProvinceID", "province_id", "id"); String provinceName = text(source, "ProvinceName", "province_name", "name");
            if (provinceId <= 0 || provinceName.isBlank()) continue;
            provinces.add(new ProvinceData(provinceId, provinceName, normalize(provinceName)));
            for (JsonNode districtSource : data(ghn.districts(provinceId))) {
                int districtId = integer(districtSource, "DistrictID", "district_id", "id"); String districtName = text(districtSource, "DistrictName", "district_name", "name");
                if (districtId <= 0 || districtName.isBlank()) continue;
                districts.add(new DistrictData(districtId, provinceId, districtName, normalize(districtName)));
                for (JsonNode wardSource : data(ghn.wards(districtId))) {
                    String wardCode = text(wardSource, "WardCode", "ward_code", "id"); String wardName = text(wardSource, "WardName", "ward_name", "name");
                    if (wardCode.isBlank() || wardName.isBlank()) continue;
                    wards.add(new WardData(localWardId(wardCode), provinceId, districtId, wardCode, wardName, normalize(wardName)));
                }
            }
        }
        return writer.replaceActiveCatalog(provinces, districts, wards);
    }

    private Iterable<JsonNode> data(JsonNode response) { JsonNode value = response == null ? null : response.path("data"); return value != null && value.isArray() ? value : List.of(); }
    private int integer(JsonNode source, String... fields) { for (String field : fields) { String value = source.path(field).asText(""); try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { } } return -1; }
    private int localWardId(String wardCode) { try { return Integer.parseInt(wardCode); } catch (NumberFormatException ignored) { return Math.max(1, Math.floorMod(wardCode.hashCode(), Integer.MAX_VALUE)); } }
    private String text(JsonNode source, String... fields) { for (String field : fields) { String value = source.path(field).asText("").trim(); if (!value.isBlank()) return value; } return ""; }
    private String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }

    public record ProvinceData(int id, String name, String normalizedName) { }
    public record DistrictData(int id, int provinceId, String name, String normalizedName) { }
    public record WardData(int id, int provinceId, int districtId, String wardCode, String name, String normalizedName) { }
}
