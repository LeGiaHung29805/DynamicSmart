package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.PurchasedCartItem;
import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.service.OrderConfirmationEventService.OrderConfirmedPayload;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.function.StreamBridge;
import tools.jackson.databind.ObjectMapper;

class OrderOutboxPublisherTests {
    private static final UUID EVENT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-10-02T04:00:00Z");

    @Test
    void orderConfirmedIsDeliveredToExistingCartContract() {
        Fixture fixture = fixture("OrderConfirmed", confirmedPayload());

        fixture.publisher.publishOne(EVENT_ID);

        verify(fixture.cart).confirm(any());
        verify(fixture.streamBridge, never()).send(any(String.class), any());
        assertEquals(OutboxEventStatus.PUBLISHED, fixture.event.getStatus());
        assertEquals(NOW, fixture.event.getPublishedAt());
    }

    @Test
    void paymentDueUsesDedicatedPaymentQueue() {
        Fixture fixture = fixture("PaymentDue", "{\"orderId\":\"" + ORDER_ID + "\"}");
        when(fixture.streamBridge.send(eq("paymentDue-out-0"), any())).thenReturn(true);

        fixture.publisher.publishOne(EVENT_ID);

        verify(fixture.streamBridge).send(eq("paymentDue-out-0"), any());
        assertEquals(OutboxEventStatus.PUBLISHED, fixture.event.getStatus());
    }

    @Test
    void regularOrderEventUsesSharedEventBusEnvelope() {
        Fixture fixture = fixture("OrderCompleted", "{\"orderId\":\"" + ORDER_ID + "\"}");
        when(fixture.streamBridge.send(eq("orderEvents-out-0"), any())).thenReturn(true);

        fixture.publisher.publishOne(EVENT_ID);

        verify(fixture.streamBridge).send(eq("orderEvents-out-0"), any());
        assertEquals(OutboxEventStatus.PUBLISHED, fixture.event.getStatus());
    }

    @Test
    void deliveryFailureKeepsPendingAndSchedulesBackoff() {
        Fixture fixture = fixture("OrderConfirmed", confirmedPayload());
        Mockito.doThrow(new IllegalStateException("cart down")).when(fixture.cart).confirm(any());

        fixture.publisher.publishOne(EVENT_ID);

        assertEquals(OutboxEventStatus.PENDING, fixture.event.getStatus());
        assertEquals(1, fixture.event.getAttemptCount());
        assertTrue(fixture.event.getAvailableAt().isAfter(NOW));
    }

    @Test
    void tenthDeliveryFailureMovesEventToTerminalFailedState() {
        Fixture fixture = fixture("OrderConfirmed", confirmedPayload());
        fixture.event.setAttemptCount(9);
        Mockito.doThrow(new IllegalStateException("cart still down")).when(fixture.cart).confirm(any());

        fixture.publisher.publishOne(EVENT_ID);

        assertEquals(OutboxEventStatus.FAILED, fixture.event.getStatus());
        assertEquals(10, fixture.event.getAttemptCount());
    }

    private Fixture fixture(String eventType, String payload) {
        OutboxEventRepository events = Mockito.mock(OutboxEventRepository.class);
        CartOrderConfirmationGateway cart = Mockito.mock(CartOrderConfirmationGateway.class);
        StreamBridge streamBridge = Mockito.mock(StreamBridge.class);
        ObjectMapper objectMapper = new ObjectMapper();
        OutboxEvent event = OutboxEvent.pending(
                EVENT_ID, "ORDER", ORDER_ID, eventType, 1, payload, CORRELATION_ID, NOW.minusSeconds(1));
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));
        OrderOutboxPublisher publisher = new OrderOutboxPublisher(
                events, cart, streamBridge, objectMapper,
                new OutboxProperties(Duration.ofSeconds(1), 100), Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(publisher, event, cart, streamBridge);
    }

    private String confirmedPayload() {
        return new ObjectMapper().writeValueAsString(new OrderConfirmedPayload(
                ORDER_ID, UUID.randomUUID(), "CART",
                List.of(new PurchasedCartItem(UUID.randomUUID(), 2, 7)), NOW.minusSeconds(30)));
    }

    private record Fixture(
            OrderOutboxPublisher publisher,
            OutboxEvent event,
            CartOrderConfirmationGateway cart,
            StreamBridge streamBridge) {
    }
}
