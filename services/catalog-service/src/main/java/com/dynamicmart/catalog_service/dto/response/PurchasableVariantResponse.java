package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record PurchasableVariantResponse(
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        String sku,
        VariantPriceResponse price,
        int availableQuantity,
        int weightGrams,
        Integer lengthCm,
        Integer widthCm,
        Integer heightCm,
        String imageUrl,
        boolean purchasable,
        String unavailableReason
) {
}
