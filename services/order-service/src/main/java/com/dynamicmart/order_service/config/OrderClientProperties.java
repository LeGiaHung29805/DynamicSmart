package com.dynamicmart.order_service.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public record OrderClientProperties(
        URI identityBaseUrl,
        URI catalogBaseUrl,
        URI cartBaseUrl,
        URI paymentBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String internalApiKey) {

    public OrderClientProperties {
        requireHttpUri(identityBaseUrl, "identity-base-url");
        requireHttpUri(catalogBaseUrl, "catalog-base-url");
        requireHttpUri(cartBaseUrl, "cart-base-url");
        requireHttpUri(paymentBaseUrl, "payment-base-url");
        requirePositive(connectTimeout, "connect-timeout");
        requirePositive(readTimeout, "read-timeout");
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new IllegalArgumentException("app.clients.internal-api-key không được để trống.");
        }
    }

    private static void requireHttpUri(URI value, String name) {
        if (value == null || value.getScheme() == null
                || !(value.getScheme().equalsIgnoreCase("http") || value.getScheme().equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("app.clients." + name + " phải là HTTP(S) URI hợp lệ.");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("app.clients." + name + " phải lớn hơn 0.");
        }
    }
}
