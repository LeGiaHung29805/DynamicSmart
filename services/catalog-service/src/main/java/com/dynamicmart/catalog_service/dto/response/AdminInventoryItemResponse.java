package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.VariantStatus;
import java.util.UUID;

public record AdminInventoryItemResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        String sku,
        VariantStatus variantStatus,
        int onHandQuantity,
        int reservedQuantity,
        int availableQuantity,
        long version
) {
}
