package com.dynamicmart.payment_service.messaging.consumer;


import java.time.Instant;
import java.util.UUID;

/** RabbitMQ contract. The top-level orderId remains optional for backward compatibility. */
public record PaymentDueMessage(
        UUID eventId,
        String eventType,
        Integer eventVersion,
        String producer,
        UUID aggregateId,
        Instant occurredAt,
        UUID correlationId,
        Payload payload,
        UUID orderId) {

    public PaymentDueEvent toDomainEvent() {
        UUID resolvedOrderId = payload != null && payload.orderId() != null ? payload.orderId()
                : orderId != null ? orderId : aggregateId;
        return new PaymentDueEvent(eventId, resolvedOrderId, correlationId);
    }

    public boolean isPaymentDue() {
        if (eventType == null || eventType.isBlank()) {
            return true;
        }
        boolean supportedVersion = eventVersion == null || eventVersion == 1;
        boolean expectedProducer = producer == null || producer.isBlank() || "order-service".equals(producer);
        return "PaymentDue".equals(eventType) && supportedVersion && expectedProducer;
    }

    public record Payload(UUID orderId) { }
}
