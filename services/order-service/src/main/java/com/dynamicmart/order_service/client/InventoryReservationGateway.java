package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

/** Idempotent reservation boundary implemented by Catalog/Inventory Service. */
public interface InventoryReservationGateway {
    Reservation reserve(ReserveInventoryRequest request);

    void release(ReleaseInventoryRequest request);

    record ReserveInventoryRequest(
            UUID operationKey,
            UUID sagaId,
            UUID correlationId,
            UUID customerId,
            UUID checkoutSessionId,
            List<InventoryLine> lines) {
        public ReserveInventoryRequest {
            lines = List.copyOf(lines);
        }
    }

    record InventoryLine(UUID productId, UUID variantId, int quantity) {
    }

    record Reservation(UUID reservationId) {
    }

    record ReleaseInventoryRequest(
            UUID operationKey,
            UUID sagaId,
            UUID correlationId,
            UUID reservationId,
            String reason) {
    }
}
