package com.dynamicmart.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.zalopay")
public record ZaloPayProperties(String appId, String key1, String key2, String createUrl,
                                String callbackUrl, String returnUrl) { }
