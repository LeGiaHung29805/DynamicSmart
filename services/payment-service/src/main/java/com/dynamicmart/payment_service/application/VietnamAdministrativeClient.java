package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** Danh mục địa giới Việt Nam dự phòng khi GHN chưa được cấu hình hoặc token GHN hết hiệu lực. */
@Component
public class VietnamAdministrativeClient {
    private final RestClient client;

    public VietnamAdministrativeClient(RestClient.Builder builder,
            @Value("${app.locations.vietnam-base-url:https://provinces.open-api.vn/api/v2}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.client = builder.requestFactory(requestFactory).baseUrl(baseUrl).build();
    }

    public JsonNode all() {
        try {
            JsonNode response = client.get().uri("/?depth=2").retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) throw unavailable();
            return response;
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private PaymentException unavailable() {
        return new PaymentException(HttpStatus.BAD_GATEWAY, "LOCATION_CATALOG_UNAVAILABLE",
                "Không thể tải danh mục Tỉnh/Thành phố và Phường/Xã lúc này.");
    }
}
