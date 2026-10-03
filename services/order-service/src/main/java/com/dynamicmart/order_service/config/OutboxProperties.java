package com.dynamicmart.order_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(Duration publishDelay, int batchSize, Duration processingLease) {
    public OutboxProperties {
        if (publishDelay == null || publishDelay.isZero() || publishDelay.isNegative()) {
            throw new IllegalArgumentException("app.outbox.publish-delay phải lớn hơn 0.");
        }
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("app.outbox.batch-size phải trong khoảng 1..1000.");
        }
        if (processingLease == null || processingLease.isZero() || processingLease.isNegative()) {
            throw new IllegalArgumentException("app.outbox.processing-lease phải lớn hơn 0.");
        }
    }
}
