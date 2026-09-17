package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.config.InternalApiProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalApiVerifier {
    private final InternalApiProperties properties;
    public InternalApiVerifier(InternalApiProperties properties) { this.properties = properties; }
    public void require(String suppliedKey) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "INTERNAL_API_NOT_CONFIGURED", "Internal API chưa được cấu hình cho môi trường này.");
        if (suppliedKey == null || !MessageDigest.isEqual(properties.apiKey().getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8))) throw new PaymentException(HttpStatus.FORBIDDEN, "INTERNAL_API_FORBIDDEN", "Không được phép gọi API nội bộ.");
    }
}
