package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.api.ShippingItemRequest;
import com.dynamicmart.payment_service.config.GhnProperties;
import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Thin GHN boundary. Secrets remain in environment variables and never cross an API response or log. */
@Component
public class GhnClient {
    public record Rate(long feeVnd, int serviceId, String serviceName, String eta) { }
    private final GhnProperties properties;
    private final RestClient restClient;

    public GhnClient(GhnProperties properties, RestClient.Builder builder) {
        this.properties = properties; this.restClient = builder.build();
    }

    public Rate quote(int districtId, String wardCode, List<ShippingItemRequest> items) {
        requireQuoteConfigured();
        int weight = items.stream().mapToInt(i -> Math.multiplyExact(i.quantity(), i.weightGrams())).sum();
        int length = items.stream().mapToInt(i -> i.lengthCm()).max().orElseThrow();
        int width = items.stream().mapToInt(i -> i.widthCm()).max().orElseThrow();
        int height = items.stream().mapToInt(i -> Math.multiplyExact(i.quantity(), i.heightCm())).sum();
        Map<String, Object> request = Map.of("from_district_id", properties.fromDistrictId(), "from_ward_code", properties.fromWardCode(), "to_district_id", districtId, "to_ward_code", wardCode,
                "service_type_id", 2, "weight", weight, "length", length, "width", width, "height", height);
        try {
            JsonNode data = restClient.post().uri(properties.baseUrl() + "/shiip/public-api/v2/shipping-order/fee")
                    .header("Token", properties.token()).header("ShopId", properties.shopId()).contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(JsonNode.class);
            if (data == null || data.path("data").path("total").isMissingNode()) throw unavailable();
            JsonNode result = data.path("data");
            return new Rate(result.path("total").asLong(-1), result.path("service_id").asInt(2), result.path("service_name").asText("GHN tiêu chuẩn"), result.path("expected_delivery_time").asText("Theo lịch GHN"));
        } catch (RestClientException exception) { throw unavailable(); }
    }

    public JsonNode provinces() { return get("/shiip/public-api/master-data/province", Map.of()); }
    public JsonNode districts(int provinceId) { return get("/shiip/public-api/master-data/district", Map.of("province_id", provinceId)); }
    public JsonNode wards(int districtId) { return get("/shiip/public-api/master-data/ward", Map.of("district_id", districtId)); }

    private JsonNode get(String path, Map<String, Object> query) {
        requireCatalogConfigured();
        try {
            var request = restClient.get().uri(builder -> { builder.path(properties.baseUrl() + path); query.forEach(builder::queryParam); return builder.build(); }).header("Token", properties.token());
            return request.retrieve().body(JsonNode.class);
        } catch (RestClientException exception) { throw unavailable(); }
    }
    private void requireCatalogConfigured() { if (properties.baseUrl() == null || properties.baseUrl().isBlank() || properties.token() == null || properties.token().isBlank()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_NOT_CONFIGURED", "GHN chưa được cấu hình cho môi trường này."); }
    private void requireQuoteConfigured() { requireCatalogConfigured(); if (properties.shopId() == null || properties.shopId().isBlank() || properties.fromDistrictId() == null || properties.fromWardCode() == null || properties.fromWardCode().isBlank()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_NOT_CONFIGURED", "Kho gửi GHN chưa được cấu hình cho môi trường này."); }
    private PaymentException unavailable() { return new PaymentException(HttpStatus.BAD_GATEWAY, "GHN_UNAVAILABLE", "GHN không phản hồi báo giá. Vui lòng thử lại."); }
}
