package com.dynamicmart.order_service.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynamicmart.order_service.exception.OrderException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class InternalApiVerifierTests {
    private static final String KEY = "test-internal-key";
    private final InternalApiVerifier verifier = new InternalApiVerifier(new OrderClientProperties(
            URI.create("http://localhost:8081"),
            URI.create("http://localhost:8082"),
            URI.create("http://localhost:8083"),
            URI.create("http://localhost:8085"),
            Duration.ofSeconds(1), Duration.ofSeconds(2), KEY));

    @Test
    void acceptsMatchingKey() {
        assertThatCode(() -> verifier.require(KEY)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrWrongKey() {
        assertThatThrownBy(() -> verifier.require(null))
                .isInstanceOf(OrderException.class)
                .extracting("code").isEqualTo("INTERNAL_API_FORBIDDEN");
        assertThatThrownBy(() -> verifier.require("wrong"))
                .isInstanceOf(OrderException.class)
                .extracting("code").isEqualTo("INTERNAL_API_FORBIDDEN");
    }
}