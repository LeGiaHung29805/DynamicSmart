package com.dynamicmart.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public record CatalogDemoProperties(String password, long tokenTtlSeconds) {
    public CatalogDemoProperties {
        password = password == null || password.isBlank() ? "Demo@123" : password;
        tokenTtlSeconds = tokenTtlSeconds < 300 ? 3600 : tokenTtlSeconds;
    }
}
