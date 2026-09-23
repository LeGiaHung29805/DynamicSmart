package com.dynamicmart.order_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.checkout")
public record CheckoutProperties(Duration sessionTtl) {
    public CheckoutProperties {
        if (sessionTtl == null || sessionTtl.isZero() || sessionTtl.isNegative()) {
            throw new IllegalArgumentException("Checkout Session TTL phải lớn hơn 0.");
        }
    }
}
