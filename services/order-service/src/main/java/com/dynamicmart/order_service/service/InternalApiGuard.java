package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.exception.OrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InternalApiGuard {
    public static final String HEADER_NAME = "X-Internal-Api-Key";

    private final String expectedKey;

    public InternalApiGuard(@Value("${app.clients.internal-api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    public void requireValid(String actualKey) {
        if (!StringUtils.hasText(expectedKey) || !expectedKey.equals(actualKey)) {
            throw new OrderException(
                    HttpStatus.FORBIDDEN,
                    "INTERNAL_API_KEY_INVALID",
                    "Internal API key không hợp lệ.");
        }
    }
}
