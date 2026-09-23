package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.ProductStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProductResponse(
        UUID id,
        CategoryBriefResponse category,
        String name,
        String slug,
        String shortDescription,
        String description,
        ProductStatus status,
        Instant publishedAt,
        boolean featured,
        Integer defaultWeightGrams,
        Integer defaultLengthCm,
        Integer defaultWidthCm,
        Integer defaultHeightCm,
        List<AttributeValueResponse> attributes,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants
) {
}
