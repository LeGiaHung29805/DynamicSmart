package com.dynamicmart.order_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway.PurchasedCartItem;
import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.service.OrderConfirmationEventService.OrderConfirmedPayload;
import com.dynamicmart.order_service.service.OrderOutboxClaimService.OutboxClaim;
import java.time.Duration;
import java.time.Instant;
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
    private static final UUID OWNER = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-02T04:00:00Z");

    @Test
    void orderConfirmedIsDeliveredToExistingCartContract() {
        Fixture fixture = fixture("OrderConfirmed", confirmedPayload());

        fixture.publisher().publishOne(EVENT_ID);

        verify(fixture.cart()).confirm(any());
        verify(fixture.streamBridge(), never()).send(any(String.class), any());
        verify(fixture.claims()).markPublished(EVENT_ID, OWNER);
    }

    @Test
    void paymentDueUsesDedicatedPaymentQueue() {
        Fixture fixture = fixture("PaymentDue", "{\"orderId\":\"" + ORDER_ID + "\"}");
        when(fixture.streamBridge().send(eq("paymentDue-out-0"), any())).thenReturn(true);

        fixture.publisher().publishOne(EVENT_ID);

        verify(fixture.streamBridge()).send(eq("paymentDue-out-0"), any());
        verify(fixture.claims()).markPublished(EVENT_ID, OWNER);
    }

    @Test
    void regularOrderEventUsesSharedEventBusEnvelope() {
        Fixture fixture = fixture("OrderCompleted", "{\"orderId\":\"" + ORDER_ID + "\"}");
        when(fixture.streamBridge().send(eq("orderEvents-out-0"), any())).thenReturn(true);

        fixture.publisher().publishOne(EVENT_ID);

        verify(fixture.streamBridge()).send(eq("orderEvents-out-0"), any());
        verify(fixture.claims()).markPublished(EVENT_ID, OWNER);
    }

    @Test
    void deliveryFailureReturnsClaimForRetry() {
        Fixture fixture = fixture("OrderConfirmed", confirmedPayload());
        Mockito.doThrow(new IllegalStateException("cart down")).when(fixture.cart()).confirm(any());

        fixture.publisher().publishOne(EVENT_ID);

        verify(fixture.claims()).markFailed(EVENT_ID, OWNER);
        verify(fixture.claims(), never()).markPublished(EVENT_ID, OWNER);
    }

    @Test
    void unavailableOrAlreadyClaimedEventIsNotDispatched() {
        Fixture fixture = fixture("OrderCompleted", "{}");
        when(fixture.claims().claim(EVENT_ID)).thenReturn(Optional.empty());

        fixture.publisher().publishOne(EVENT_ID);

        verify(fixture.streamBridge(), never()).send(any(String.class), any());
        verify(fixture.claims(), never()).markPublished(any(), any());
        verify(fixture.claims(), never()).markFailed(any(), any());
    }

    private Fixture fixture(String eventType, String payload) {
        OrderOutboxClaimService claims = Mockito.mock(OrderOutboxClaimService.class);
        CartOrderConfirmationGateway cart = Mockito.mock(CartOrderConfirmationGateway.class);
        StreamBridge streamBridge = Mockito.mock(StreamBridge.class);
        OutboxClaim claim = new OutboxClaim(
                EVENT_ID, OWNER, ORDER_ID, eventType, 1, payload, CORRELATION_ID, NOW.minusSeconds(1));
        when(claims.claim(EVENT_ID)).thenReturn(Optional.of(claim));
        OrderOutboxPublisher publisher = new OrderOutboxPublisher(
                claims, cart, streamBridge, new ObjectMapper(),
                new OutboxProperties(Duration.ofSeconds(1), 100, Duration.ofSeconds(30)));
        return new Fixture(publisher, claims, cart, streamBridge);
    }

    private String confirmedPayload() {
        return new ObjectMapper().writeValueAsString(new OrderConfirmedPayload(
                ORDER_ID, UUID.randomUUID(), "CART",
                List.of(new PurchasedCartItem(UUID.randomUUID(), 2, 7)), NOW.minusSeconds(30)));
    }

    private record Fixture(
            OrderOutboxPublisher publisher,
            OrderOutboxClaimService claims,
            CartOrderConfirmationGateway cart,
            StreamBridge streamBridge) {
    }
}
