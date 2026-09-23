package com.dynamicmart.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        CategoryBriefResponse category,
        ProductImageResponse primaryImage,
        UUID representativeVariantId,
        VariantPriceResponse representativePrice,
        long minimumListPriceVnd,
        long maximumListPriceVnd,
        boolean inStock,
        boolean featured,
        boolean bestSeller,
        boolean voucherEligible,
        Instant publishedAt
) {
}
