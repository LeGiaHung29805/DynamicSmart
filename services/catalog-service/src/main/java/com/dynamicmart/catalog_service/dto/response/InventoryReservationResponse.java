package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID reservationId,
        UUID checkoutSessionId,
        UUID orderId,
        InventoryReservationStatus status,
        Instant expiresAt,
        Instant committedAt,
        Instant releasedAt,
        String releaseReason,
        List<InventoryReservationItemResponse> items
) {
}
