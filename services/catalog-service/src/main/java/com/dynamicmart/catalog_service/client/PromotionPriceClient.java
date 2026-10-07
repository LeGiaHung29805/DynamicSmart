package com.dynamicmart.catalog_service.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PromotionPriceClient {
    private final RestClient restClient;

    public PromotionPriceClient(
            @Value("${app.clients.cart-service-url:http://localhost:8083}") String cartServiceUrl,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(cartServiceUrl).build();
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
