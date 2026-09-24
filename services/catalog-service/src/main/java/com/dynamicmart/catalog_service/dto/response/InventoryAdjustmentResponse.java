package com.dynamicmart.catalog_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InventoryAdjustmentResponse(
        UUID id,
        UUID operationKey,
        UUID variantId,
        int quantityDelta,
        int onHandBefore,
        int onHandAfter,
        String reason,
        UUID actorAdminId,
        Instant createdAt
) {
}
