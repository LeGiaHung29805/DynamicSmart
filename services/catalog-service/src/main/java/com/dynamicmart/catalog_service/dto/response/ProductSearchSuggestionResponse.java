package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record ProductSearchSuggestionResponse(
        UUID id,
        String name,
        String slug,
        String categoryName,
        String categorySlug,
        String imageUrl,
        Long minimumPriceVnd
) {
}
