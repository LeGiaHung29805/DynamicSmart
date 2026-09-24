package com.dynamicmart.catalog_service.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void productIsPublicOnlyAfterPublicationTime() {
        Product product = new Product(UUID.randomUUID(), UUID.randomUUID(), "Laptop", "laptop",
                null, null, 1000, 20, 20, 10);
        Instant now = Instant.parse("2026-09-24T00:00:00Z");

        product.publish(now.plusSeconds(60));
        assertFalse(product.isPublicAt(now));
        assertTrue(product.isPublicAt(now.plusSeconds(60)));

        product.deactivate();
        assertFalse(product.isPublicAt(now.plusSeconds(120)));
    }
}
