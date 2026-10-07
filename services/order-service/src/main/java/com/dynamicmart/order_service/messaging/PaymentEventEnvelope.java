package com.dynamicmart.order_service.messaging;

import java.time.Instant;
import java.util.UUID;

/** Version 1 envelope published by Payment Service through dynamicmart.events. */
public record PaymentEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        UUID aggregateId,
        Instant occurredAt,
        UUID correlationId,
        PaymentPayload payload) {

    public record PaymentPayload(
            UUID paymentId,
            UUID orderId,
            UUID customerId,
            long amountVnd,
            String timing,
            String method,
            String status) {
    }
}
