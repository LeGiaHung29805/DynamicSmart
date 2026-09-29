package com.dynamicmart.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sepay")
public record SePayProperties(String webhookSecret, String bankId, String accountNo,
                              String accountName, String qrBaseUrl) { }
