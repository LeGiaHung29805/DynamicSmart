package com.dynamicmart.order_service.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.saga-recovery")
public record SagaRecoveryProperties(
        boolean enabled,
        Duration fixedDelay,
        Duration initialDelay,
        Duration staleAfter,
        Duration leaseDuration,
        Duration retryBackoff,
        int batchSize) {

    public SagaRecoveryProperties {
        requirePositive(fixedDelay, "fixed-delay");
        requireNonNegative(initialDelay, "initial-delay");
        requirePositive(staleAfter, "stale-after");
        requirePositive(leaseDuration, "lease-duration");
        requirePositive(retryBackoff, "retry-backoff");
        if (batchSize <= 0 || batchSize > 1000) {
            throw new IllegalArgumentException("app.saga-recovery.batch-size phải trong khoảng 1..1000.");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("app.saga-recovery." + name + " phải lớn hơn 0.");
        }
    }

    private static void requireNonNegative(Duration value, String name) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException("app.saga-recovery." + name + " không được âm.");
        }
    }
}
