package com.dynamicmart.catalog_service.config;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("catalog-security-test")
class CatalogSecurityTestController {
    @GetMapping("/actuator/health")
    String health() {
        return "UP";
    }

    @GetMapping("/api/v1/catalog/products")
    String publicCatalog() {
        return "public";
    }

    @GetMapping("/api/v1/catalog/admin/products")
    String adminCatalog() {
        return "admin";
    }

    @GetMapping("/api/v1/catalog/internal/ping")
    String internalCatalog() {
        return "internal";
    }

    @GetMapping("/api/v1/catalog/private")
    String authenticatedCatalog() {
        return "authenticated";
    }
}
