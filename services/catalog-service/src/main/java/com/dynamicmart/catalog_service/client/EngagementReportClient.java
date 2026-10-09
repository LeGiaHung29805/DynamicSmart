package com.dynamicmart.catalog_service.client;

import com.dynamicmart.catalog_service.config.CatalogClientProperties;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class EngagementReportClient {
    private final RestClient restClient;
    private final List<UUID> fallbackBestSellerIds;

    public EngagementReportClient(CatalogClientProperties properties,
                                  @Value("${app.internal.api-key:}") String internalApiKey,
                                  RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.engagementConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.engagementReadTimeoutMs()));
        RestClient.Builder clientBuilder = builder.baseUrl(properties.engagementServiceUrl())
                .requestFactory(requestFactory);
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            clientBuilder.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        this.restClient = clientBuilder.build();
        this.fallbackBestSellerIds = properties.bestSellerFallbackProductIds();
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
    public record BestSellerResponse(UUID productId, UUID variantId, long quantitySold,
                                     long grossSalesVnd, long netItemSalesVnd) {}
}
