package com.dynamicmart.catalog_service.config;

import com.dynamicmart.catalog_service.exception.CatalogException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class InternalApiVerifier {
    private final InternalApiProperties properties;

    public InternalApiVerifier(InternalApiProperties properties) {
        this.properties = properties;
    }

    public void require(String suppliedKey) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new CatalogException(HttpStatus.SERVICE_UNAVAILABLE,
                    "INTERNAL_API_NOT_CONFIGURED", "Internal API chưa được cấu hình.");
        }
        boolean valid = suppliedKey != null && MessageDigest.isEqual(
                properties.apiKey().getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new CatalogException(HttpStatus.FORBIDDEN,
                    "INTERNAL_API_FORBIDDEN", "Không được phép gọi API nội bộ.");
        }
    }
}
