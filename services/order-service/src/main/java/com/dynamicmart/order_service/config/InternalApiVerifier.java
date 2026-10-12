package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.exception.OrderException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalApiVerifier {
    private final OrderClientProperties properties;

    public InternalApiVerifier(OrderClientProperties properties) {
        this.properties = properties;
    }

    public void require(String suppliedKey) {
        String expectedKey = properties.internalApiKey();
        boolean valid = suppliedKey != null && MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new OrderException(HttpStatus.FORBIDDEN, "INTERNAL_API_FORBIDDEN",
                    "Không được phép gọi API nội bộ.");
        }
    }
}