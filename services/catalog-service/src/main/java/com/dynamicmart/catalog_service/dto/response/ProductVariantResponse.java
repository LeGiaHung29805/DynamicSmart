package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.VariantStatus;
import java.util.List;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        String sku,
        String name,
        VariantPriceResponse price,
        int weightGrams,
        Integer lengthCm,
        Integer widthCm,
        Integer heightCm,
        VariantStatus status,
        int sortOrder,
        InventoryAvailabilityResponse inventory,
        boolean purchasable,
        List<AttributeValueResponse> attributes,
        List<ProductImageResponse> images
) {
}
