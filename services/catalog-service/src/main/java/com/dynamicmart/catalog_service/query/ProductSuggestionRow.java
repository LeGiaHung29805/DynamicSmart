package com.dynamicmart.catalog_service.query;

import java.util.UUID;

public record ProductSuggestionRow(
        UUID id,
        String name,
        String slug,
        String categoryName,
        String categorySlug,
        String imageUrl,
        Long minimumPriceVnd
) {
}
