package com.dynamicmart.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vnpay")
public record VnPayProperties(String tmnCode, String paymentUrl, String returnUrl, String hashSecret) { }
