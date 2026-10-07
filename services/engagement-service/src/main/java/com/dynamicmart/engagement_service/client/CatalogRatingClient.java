package com.dynamicmart.engagement_service.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogRatingClient {
    private final RestClient restClient;

    public CatalogRatingClient(@Value("${app.clients.catalog-service-url:http://localhost:8082}") String catalogServiceUrl,
                               @Value("${app.security.internal-api-key:}") String internalApiKey,
                               RestClient.Builder builder) {
        RestClient.Builder clientBuilder = builder.baseUrl(catalogServiceUrl);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            clientBuilder.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        this.restClient = clientBuilder.build();
    }

    public void updateRating(UUID productId, double averageRating, int reviewCount) {
        try {
            restClient.put()
                    .uri("/api/v1/catalog/internal/products/{productId}/rating", productId)
                    .body(new UpdateRatingRequest(averageRating, reviewCount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ignored) {
            // Log error here in real-world, but for now we ignore or we could retry
        }
    }

    public record UpdateRatingRequest(double averageRating, int reviewCount) {}
}
