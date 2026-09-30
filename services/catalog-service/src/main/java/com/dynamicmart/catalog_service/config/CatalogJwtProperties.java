package com.dynamicmart.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record CatalogJwtProperties(String issuer, String hmacSecretBase64) {
}
