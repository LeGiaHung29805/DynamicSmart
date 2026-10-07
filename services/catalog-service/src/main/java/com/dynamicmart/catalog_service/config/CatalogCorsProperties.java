package com.dynamicmart.catalog_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CatalogCorsProperties(String frontendOrigin) {
    public CatalogCorsProperties {
        frontendOrigin = frontendOrigin == null || frontendOrigin.isBlank()
                ? "http://localhost:3000" : frontendOrigin.replaceAll("/+$", "");
    }
}
