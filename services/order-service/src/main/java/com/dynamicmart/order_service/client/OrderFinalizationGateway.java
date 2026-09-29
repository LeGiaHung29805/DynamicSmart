package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

public interface OrderFinalizationGateway {
    void finalizeConfirmed(FinalizationRequest request);

    record PurchasedCartItem(UUID id, int quantity, long version) { }

    record FinalizationRequest(
            UUID orderId, UUID customerId, UUID correlationId, UUID sagaId,
            UUID inventoryReservationId, List<PurchasedCartItem> items) {
        public FinalizationRequest { items = List.copyOf(items); }
    }
}
