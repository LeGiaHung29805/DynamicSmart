package com.dynamicmart.order_service.client;

import java.util.List;
import java.util.UUID;

public interface CartOrderConfirmationGateway {
    void confirm(OrderConfirmedCommand command);

    record OrderConfirmedCommand(
            UUID eventId,
            UUID correlationId,
            UUID customerId,
            String source,
            List<PurchasedCartItem> items) {
    }

    record PurchasedCartItem(UUID id, int quantity, long version) {
    }
}
