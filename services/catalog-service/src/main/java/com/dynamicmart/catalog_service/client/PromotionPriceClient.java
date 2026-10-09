package com.dynamicmart.catalog_service.client;

import com.dynamicmart.catalog_service.config.CatalogPromotionProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PromotionPriceClient {
    private final RestClient restClient;

    public PromotionPriceClient(CatalogPromotionProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        this.restClient = builder.baseUrl(properties.cartServiceUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public List<PriceResponse> resolve(List<PriceRequest> variants) {
        if (variants.isEmpty()) return List.of();
        try {
            List<PriceResponse> response = restClient.post()
                    .uri("/api/v1/cart/promotions/prices/resolve")
                    .body(new BatchRequest(variants))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PriceResponse>>() { });
            return response == null ? List.of() : response;
        } catch (RestClientException exception) {
            return List.of();
        }
    }

    public record BatchRequest(List<PriceRequest> variants) { }
    public record PriceRequest(UUID variantId, long listPriceVnd) { }
    public record PriceResponse(UUID variantId, long listPriceVnd, long discountVnd, long salePriceVnd,
                                Integer discountPercent, UUID promotionId, String promotionName, Instant endsAt) { }
}
