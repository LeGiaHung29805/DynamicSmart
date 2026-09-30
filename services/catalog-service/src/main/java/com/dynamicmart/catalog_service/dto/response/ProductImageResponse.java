package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String imageUrl,
        String altText,
        int sortOrder,
        boolean primary
) {
}
