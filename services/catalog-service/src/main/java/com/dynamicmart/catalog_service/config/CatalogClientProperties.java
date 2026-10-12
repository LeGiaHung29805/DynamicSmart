package com.dynamicmart.catalog_service.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record CatalogClientProperties(
        String reportingServiceUrl,
        int reportingConnectTimeoutMs,
        int reportingReadTimeoutMs,
        List<UUID> bestSellerFallbackProductIds
) {
    public CatalogClientProperties {
        reportingServiceUrl = normalizeUrl(reportingServiceUrl, "http://localhost:8088");
        reportingConnectTimeoutMs = positiveOrDefault(reportingConnectTimeoutMs, 1000);
        reportingReadTimeoutMs = positiveOrDefault(reportingReadTimeoutMs, 1500);
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
