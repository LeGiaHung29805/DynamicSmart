package com.dynamicmart.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ghn")
public record GhnProperties(String baseUrl, String token, String shopId, Integer fromDistrictId, Integer quoteTtlMinutes) { }
