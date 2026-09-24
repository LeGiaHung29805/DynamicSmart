package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record InventoryReservationItemResponse(
        UUID variantId,
        int quantity,
        int availableQuantityAfter
) {
}
