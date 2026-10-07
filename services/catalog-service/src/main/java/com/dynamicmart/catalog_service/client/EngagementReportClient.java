package com.dynamicmart.catalog_service.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class EngagementReportClient {
    private final RestClient restClient;
    private final List<UUID> fallbackBestSellerIds;

    public EngagementReportClient(@Value("${app.clients.engagement-service-url:http://localhost:8086}") String engagementServiceUrl,
                                  @Value("${app.internal.api-key:}") String internalApiKey,
                                  @Value("${app.clients.best-seller-fallback-product-ids:}") String fallbackProductIds,
                                  RestClient.Builder builder) {
        RestClient.Builder clientBuilder = builder.baseUrl(engagementServiceUrl);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            clientBuilder.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        this.restClient = clientBuilder.build();
        this.fallbackBestSellerIds = java.util.Arrays.stream(fallbackProductIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .toList();
    }

    public List<UUID> getBestSellers(LocalDate from, LocalDate to, int limit) {
        try {
            String uri = UriComponentsBuilder.fromPath("/api/v1/reports/products/best-sellers")
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .queryParam("limit", limit)
                    .build().toUriString();

            ApiResponse<List<BestSellerResponse>> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<List<BestSellerResponse>>>() {});

            if (response != null && response.data() != null) {
                return response.data().stream().map(BestSellerResponse::productId).toList();
            }
            return fallback(limit);
        } catch (RestClientException exception) {
            return fallback(limit);
        }
    }

    private List<UUID> fallback(int limit) {
        return fallbackBestSellerIds.stream().limit(Math.max(limit, 0)).toList();
    }

    public record ApiResponse<T>(T data) {}
    public record BestSellerResponse(UUID productId, long soldQuantity) {}
}
