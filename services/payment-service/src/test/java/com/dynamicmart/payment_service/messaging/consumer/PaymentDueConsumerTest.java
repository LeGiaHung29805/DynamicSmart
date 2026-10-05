package com.dynamicmart.payment_service.messaging.consumer;

import com.dynamicmart.payment_service.service.PaymentDueService;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentDueConsumerTest {
    @Test
    void mapsStandardEventEnvelopeToDomainEvent() {
        PaymentDueService service = mock(PaymentDueService.class);
        PaymentDueConsumer consumer = new PaymentDueConsumer(service);
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentDueMessage message = new PaymentDueMessage(
                eventId, "PaymentDue", 1, "order-service", orderId, Instant.now(), correlationId,
                new PaymentDueMessage.Payload(orderId), null);

        consumer.paymentDue().accept(message);

        verify(service).consume(new PaymentDueEvent(eventId, orderId, correlationId));
    }

    @Test
    void keepsBackwardCompatibilityWithFlatMessage() {
        PaymentDueService service = mock(PaymentDueService.class);
        PaymentDueConsumer consumer = new PaymentDueConsumer(service);
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        consumer.paymentDue().accept(new PaymentDueMessage(
                eventId, null, null, null, null, null, null, null, orderId));

        verify(service).consume(new PaymentDueEvent(eventId, orderId, null));
    }

    @Test
    void ignoresUnexpectedEventTypeOnDedicatedBinding() {
        PaymentDueService service = mock(PaymentDueService.class);
        PaymentDueConsumer consumer = new PaymentDueConsumer(service);

        consumer.paymentDue().accept(new PaymentDueMessage(
                UUID.randomUUID(), "OrderCreated", 1, "order-service", UUID.randomUUID(),
                Instant.now(), UUID.randomUUID(), null, null));

        verify(service, never()).consume(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresUnsupportedEnvelopeVersionOrProducer() {
        PaymentDueService service = mock(PaymentDueService.class);
        PaymentDueConsumer consumer = new PaymentDueConsumer(service);
        UUID orderId = UUID.randomUUID();

        consumer.paymentDue().accept(new PaymentDueMessage(
                UUID.randomUUID(), "PaymentDue", 2, "order-service", orderId,
                Instant.now(), UUID.randomUUID(), new PaymentDueMessage.Payload(orderId), null));
        consumer.paymentDue().accept(new PaymentDueMessage(
                UUID.randomUUID(), "PaymentDue", 1, "unknown-service", orderId,
                Instant.now(), UUID.randomUUID(), new PaymentDueMessage.Payload(orderId), null));

        verify(service, never()).consume(org.mockito.ArgumentMatchers.any());
    }
}
