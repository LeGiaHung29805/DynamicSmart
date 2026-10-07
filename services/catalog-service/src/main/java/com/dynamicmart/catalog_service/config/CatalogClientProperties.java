package com.dynamicmart.catalog_service.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record CatalogClientProperties(
        String engagementServiceUrl,
        int engagementConnectTimeoutMs,
        int engagementReadTimeoutMs,
        List<UUID> bestSellerFallbackProductIds
) {
    public CatalogClientProperties {
        engagementServiceUrl = normalizeUrl(engagementServiceUrl, "http://localhost:8086");
        engagementConnectTimeoutMs = positiveOrDefault(engagementConnectTimeoutMs, 1000);
        engagementReadTimeoutMs = positiveOrDefault(engagementReadTimeoutMs, 1500);
        bestSellerFallbackProductIds = bestSellerFallbackProductIds == null
                ? List.of() : List.copyOf(bestSellerFallbackProductIds);
    }

    private static String normalizeUrl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.replaceAll("/+$", "");
    }

    private static int positiveOrDefault(int value, int fallback) {
        return value <= 0 ? fallback : value;
    }
}
