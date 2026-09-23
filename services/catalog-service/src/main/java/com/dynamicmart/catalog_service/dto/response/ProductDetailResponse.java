package com.dynamicmart.catalog_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        CategoryBriefResponse category,
        boolean featured,
        Instant publishedAt,
        List<AttributeValueResponse> attributes,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants
) {
}
