package com.dynamicmart.engagement_service.service;

import com.dynamicmart.engagement_service.exception.EngagementException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InternalApiGuard {
    private final String expectedKey;

    public InternalApiGuard(@Value("${app.security.internal-api-key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    public void requireValid(String actualKey) {
        if (!StringUtils.hasText(expectedKey) || !expectedKey.equals(actualKey)) {
            throw new EngagementException(HttpStatus.FORBIDDEN, "INTERNAL_API_KEY_INVALID",
                    "Internal API key không hợp lệ.");
        }
    }
}
