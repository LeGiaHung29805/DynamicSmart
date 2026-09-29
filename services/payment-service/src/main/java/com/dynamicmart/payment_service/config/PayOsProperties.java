package com.dynamicmart.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payos")
public record PayOsProperties(String clientId, String apiKey, String checksumKey, String baseUrl,
                              String returnUrl, String cancelUrl) { }
