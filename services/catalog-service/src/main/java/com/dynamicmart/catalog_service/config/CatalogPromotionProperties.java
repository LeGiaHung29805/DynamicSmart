package com.dynamicmart.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.promotion")
public record CatalogPromotionProperties(
        Mode mode,
        String cartServiceUrl,
        int connectTimeoutMs,
        int readTimeoutMs,
        int demoDiscountPercent,
        int demoDurationMinutes
) {
    public enum Mode { HTTP, DEMO, DISABLED }

    public CatalogPromotionProperties {
        mode = mode == null ? Mode.HTTP : mode;
        cartServiceUrl = cartServiceUrl == null || cartServiceUrl.isBlank()
                ? "http://localhost:8083" : cartServiceUrl.replaceAll("/+$", "");
        connectTimeoutMs = connectTimeoutMs <= 0 ? 1000 : connectTimeoutMs;
        readTimeoutMs = readTimeoutMs <= 0 ? 1500 : readTimeoutMs;
        demoDiscountPercent = demoDiscountPercent < 1 || demoDiscountPercent > 90
                ? 15 : demoDiscountPercent;
        demoDurationMinutes = demoDurationMinutes <= 0 ? 120 : demoDurationMinutes;
    }
}
