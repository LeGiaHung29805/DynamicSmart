package com.dynamicmart.payment_service.client;

import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.dto.request.ShippingItemRequest;
import com.dynamicmart.payment_service.config.GhnProperties;
import tools.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/** Thin GHN boundary. Secrets remain in environment variables and never cross an API response or log. */
@Component
public class GhnClient {
    private static final Logger log = LoggerFactory.getLogger(GhnClient.class);
    public record Rate(long feeVnd, int serviceId, String serviceName, String eta) { }
    private final GhnProperties properties;
    private final RestClient restClient;

    public GhnClient(GhnProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    public Rate quote(int districtId, String wardCode, List<ShippingItemRequest> items) {
        requireQuoteConfigured();
        long totalWeight = items.stream().mapToLong(i -> (long) i.quantity() * i.weightGrams()).sum();
        int length = items.stream().mapToInt(i -> i.lengthCm()).max().orElseThrow();
        int width = items.stream().mapToInt(i -> i.widthCm()).max().orElseThrow();
        long totalHeight = items.stream().mapToLong(i -> (long) i.quantity() * i.heightCm()).sum();
        if (totalWeight > Integer.MAX_VALUE || totalHeight > Integer.MAX_VALUE) {
            throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "GHN_PACKAGE_SIZE_INVALID", "Khối lượng hoặc kích thước kiện hàng vượt giới hạn hỗ trợ.");
        }
        int weight = Math.toIntExact(totalWeight);
        int height = Math.toIntExact(totalHeight);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("from_district_id", properties.fromDistrictId());
        request.put("from_ward_code", properties.fromWardCode());
        request.put("to_district_id", districtId);
        request.put("to_ward_code", wardCode);
        request.put("service_type_id", 2);
        request.put("insurance_value", 0);
        request.put("weight", weight);
        request.put("length", length);
        request.put("width", width);
        request.put("height", height);
        try {
            JsonNode data = restClient.post().uri(properties.baseUrl() + "/shiip/public-api/v2/shipping-order/fee")
                    .header("Token", properties.token()).header("ShopId", properties.shopId())
                    .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                    .body(request).retrieve().body(JsonNode.class);
            if (data == null || data.path("code").asInt(200) != 200 || data.path("data").path("total").isMissingNode()) {
                log.warn("GHN fee response was invalid: {}", safeBody(data == null ? "null" : data.toString()));
                throw unavailable("GHN trả về dữ liệu báo giá không hợp lệ.");
            }
            JsonNode result = data.path("data");
            return new Rate(result.path("total").asLong(-1), result.path("service_id").asInt(2), result.path("service_name").asText("GHN tiêu chuẩn"), result.path("expected_delivery_time").asText("Theo lịch GHN"));
        } catch (RestClientResponseException exception) {
            log.warn("GHN fee request rejected with HTTP {}: {}", exception.getStatusCode().value(), safeBody(exception.getResponseBodyAsString()));
            throw unavailable("GHN từ chối báo giá (HTTP " + exception.getStatusCode().value() + ").");
        } catch (RestClientException exception) {
            log.warn("GHN fee request failed: {}", exception.getClass().getSimpleName());
            throw unavailable("Không thể kết nối tới GHN để lấy báo giá.");
        }
    }

    public JsonNode provinces() { return get("/shiip/public-api/master-data/province", Map.of()); }
    public JsonNode districts(int provinceId) { return get("/shiip/public-api/master-data/district", Map.of("province_id", provinceId)); }
    public JsonNode wards(int districtId) { return get("/shiip/public-api/master-data/ward", Map.of("district_id", districtId)); }

    public boolean isCatalogConfigured() {
        return properties.baseUrl() != null && !properties.baseUrl().isBlank()
                && properties.token() != null && !properties.token().isBlank();
    }

    private JsonNode get(String path, Map<String, Object> query) {
        requireCatalogConfigured();
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(properties.baseUrl()).path(path);
            query.forEach(uriBuilder::queryParam);
            URI uri = uriBuilder.build().encode().toUri();
            JsonNode response = restClient.get().uri(uri).header("Token", properties.token()).retrieve().body(JsonNode.class);
            if (response == null || !response.path("data").isArray()) throw unavailable();
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("GHN catalog request rejected with HTTP {}: {}", exception.getStatusCode().value(), safeBody(exception.getResponseBodyAsString()));
            throw unavailable("GHN từ chối yêu cầu địa giới (HTTP " + exception.getStatusCode().value() + ").");
        } catch (RestClientException exception) {
            log.warn("GHN catalog request failed: {}", exception.getClass().getSimpleName());
            throw unavailable("Không thể kết nối tới danh mục địa giới GHN.");
        }
    }
    private void requireCatalogConfigured() { if (!isCatalogConfigured()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_NOT_CONFIGURED", "GHN chưa được cấu hình cho môi trường này."); }
    private void requireQuoteConfigured() { requireCatalogConfigured(); if (properties.shopId() == null || properties.shopId().isBlank() || properties.fromDistrictId() == null || properties.fromWardCode() == null || properties.fromWardCode().isBlank()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_NOT_CONFIGURED", "Kho gửi GHN chưa được cấu hình cho môi trường này."); }
    private PaymentException unavailable() { return unavailable("GHN không phản hồi báo giá. Vui lòng thử lại."); }
    private PaymentException unavailable(String message) { return new PaymentException(HttpStatus.BAD_GATEWAY, "GHN_UNAVAILABLE", message); }
    private String safeBody(String body) {
        if (body == null || body.isBlank()) return "<empty>";
        String compact = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500) + "...";
    }
}
