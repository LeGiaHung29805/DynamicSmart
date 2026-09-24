package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutItem;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutSelection;
import com.dynamicmart.order_service.config.CheckoutProperties;
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.UpdateCheckoutSessionRequest;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CheckoutSessionServiceTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID CART_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final Instant NOW = Instant.parse("2026-09-23T09:00:00Z");

    @Test
    void createsCartSessionFromTrustedSnapshotOnly() {
        Fixture fixture = fixture();
        TrustedCheckoutItem trustedItem = cartItem();
        when(fixture.gateway.loadSelectedCartItems(CUSTOMER_ID, CART_ID))
                .thenReturn(new TrustedCheckoutSelection(CART_ID, List.of(trustedItem)));

        var response = fixture.service.create(CUSTOMER_ID,
                new CreateCheckoutSessionRequest(CheckoutSource.CART, CART_ID, null, null));

        assertEquals(CheckoutStatus.ACTIVE, response.status());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), response.expiresAt());
        assertEquals(90_000, response.items().get(0).unitPriceVnd());
        ArgumentCaptor<CheckoutSession> sessionCaptor = ArgumentCaptor.forClass(CheckoutSession.class);
        AtomicReference<CheckoutSessionItem> savedItem = new AtomicReference<>();
        verify(fixture.sessions).save(sessionCaptor.capture());
        verify(fixture.items).saveAll(argThat(items -> captureOnlyItem(items, savedItem)));
        assertEquals(64, sessionCaptor.getValue().getSelectionFingerprint().length());
        assertEquals(CART_ITEM_ID, savedItem.get().getSourceCartItemId());
        assertEquals(3L, savedItem.get().getSourceCartItemVersion());
    }

    @Test
    void rejectsClientItemDataForCartBeforeCallingGateway() {
        Fixture fixture = fixture();

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.create(CUSTOMER_ID,
                new CreateCheckoutSessionRequest(CheckoutSource.CART, CART_ID, VARIANT_ID, 1)));

        assertEquals("INVALID_CHECKOUT_SOURCE", exception.getCode());
        verify(fixture.gateway, never()).loadSelectedCartItems(any(), any());
        verify(fixture.sessions, never()).save(any());
    }

    @Test
    void rejectsBuyNowSnapshotWhenItDoesNotMatchRequestedVariantAndQuantity() {
        Fixture fixture = fixture();
        when(fixture.gateway.loadBuyNowItem(CUSTOMER_ID, VARIANT_ID, 2)).thenReturn(
                new TrustedCheckoutSelection(null, List.of(buyNowItem(1))));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.create(CUSTOMER_ID,
                new CreateCheckoutSessionRequest(CheckoutSource.BUY_NOW, null, VARIANT_ID, 2)));

        assertEquals("INVALID_TRUSTED_SELECTION", exception.getCode());
        verify(fixture.sessions, never()).save(any());
    }

    @Test
    void rejectsUpdateAfterOrderCreationAdmission() {
        Fixture fixture = fixture();
        CheckoutSession session = activeSession();
        when(fixture.sessions.findOwnedForUpdate(session.getId(), CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.sagas.existsByCheckoutSessionId(session.getId())).thenReturn(true);

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.update(
                CUSTOMER_ID, session.getId(),
                new UpdateCheckoutSessionRequest(UUID.randomUUID(), null, null)));

        assertEquals("ORDER_CREATION_IN_PROGRESS", exception.getCode());
        verify(fixture.shippingQuotes, never()).findAllByCheckoutSessionIdAndStatus(any(), any());
    }

    @Test
    void rejectsCancelAfterOrderCreationAdmission() {
        Fixture fixture = fixture();
        CheckoutSession session = activeSession();
        when(fixture.sessions.findOwnedForUpdate(session.getId(), CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.sagas.existsByCheckoutSessionId(session.getId())).thenReturn(true);

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.cancel(CUSTOMER_ID, session.getId()));

        assertEquals("ORDER_CREATION_IN_PROGRESS", exception.getCode());
        assertEquals(CheckoutStatus.ACTIVE, session.getStatus());
    }

    @Test
    void expiresSessionBeforeRejectingAnUpdate() {
        Fixture fixture = fixture();
        CheckoutSession session = CheckoutSession.create(UUID.randomUUID(), CUSTOMER_ID, CheckoutSource.CART, CART_ID,
                "a".repeat(64), NOW.minusSeconds(1), NOW.minus(Duration.ofMinutes(16)));
        when(fixture.sessions.findOwnedForUpdate(session.getId(), CUSTOMER_ID)).thenReturn(Optional.of(session));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.update(CUSTOMER_ID, session.getId(),
                new UpdateCheckoutSessionRequest(UUID.randomUUID(), null, null)));

        assertEquals("CHECKOUT_SESSION_EXPIRED", exception.getCode());
        assertEquals(CheckoutStatus.EXPIRED, session.getStatus());
    }

    @Test
    void rejectsPaymentCombinationsThatMustBeDerivedByBackend() {
        Fixture fixture = fixture();

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.update(CUSTOMER_ID,
                UUID.randomUUID(), new UpdateCheckoutSessionRequest(null, PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE)));

        assertEquals("INVALID_PAYMENT_SELECTION", exception.getCode());
        verify(fixture.sessions, never()).findOwnedForUpdate(any(), any());
    }

    @Test
    void changingAddressInvalidatesActiveQuoteAndDerivedPreview() {
        Fixture fixture = fixture();
        UUID oldAddress = UUID.randomUUID();
        UUID newAddress = UUID.randomUUID();
        CheckoutSession session = CheckoutSession.create(UUID.randomUUID(), CUSTOMER_ID, CheckoutSource.CART, CART_ID,
                "a".repeat(64), NOW.plusSeconds(900), NOW);
        session.setAddressId(oldAddress);
        session.setFinalTotalVnd(120_000L);
        CheckoutShippingQuote quote = CheckoutShippingQuote.create(
                UUID.randomUUID(), session.getId(), UUID.randomUUID(), "b".repeat(64),
                30_000, 0, 30_000, 53320, "GHN Standard", "2-3 ngày",
                200, 20, 10, 5, 201, 301, "Hà Nội", "Phường A", NOW.plusSeconds(900), NOW);
        when(fixture.sessions.findOwnedForUpdate(session.getId(), CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.shippingQuotes.findAllByCheckoutSessionIdAndStatus(
                session.getId(), com.dynamicmart.order_service.entity.ShippingQuoteStatus.ACTIVE))
                .thenReturn(List.of(quote));
        when(fixture.items.findAllByCheckoutSessionId(session.getId())).thenReturn(List.of());

        fixture.service.update(CUSTOMER_ID, session.getId(), new UpdateCheckoutSessionRequest(newAddress, null, null));

        assertEquals(com.dynamicmart.order_service.entity.ShippingQuoteStatus.INVALIDATED, quote.getStatus());
        assertEquals(newAddress, session.getAddressId());
        assertEquals(null, session.getFinalTotalVnd());
        verify(fixture.sessionVouchers).deleteAllByCheckoutSessionId(session.getId());
    }

    @Test
    void cancellingAnAlreadyCancelledSessionIsIdempotentAndKeepsSnapshots() {
        Fixture fixture = fixture();
        CheckoutSession session = CheckoutSession.create(UUID.randomUUID(), CUSTOMER_ID, CheckoutSource.CART, CART_ID,
                "a".repeat(64), NOW.plus(Duration.ofMinutes(15)), NOW);
        session.setStatus(CheckoutStatus.CANCELLED);
        CheckoutSessionItem snapshot = CheckoutSessionItem.create(UUID.randomUUID(), session.getId(), CART_ITEM_ID, 3L,
                PRODUCT_ID, VARIANT_ID, "SKU-1", "Product", "Blue", null,
                100_000, null, 10_000, 1, 200, 20, 10, 5, NOW);
        when(fixture.sessions.findOwnedForUpdate(session.getId(), CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.items.findAllByCheckoutSessionId(session.getId())).thenReturn(List.of(snapshot));

        var response = fixture.service.cancel(CUSTOMER_ID, session.getId());

        assertEquals(CheckoutStatus.CANCELLED, response.status());
        assertEquals(1, response.items().size());
        verify(fixture.items, never()).deleteAllByCheckoutSessionId(any());
        verify(fixture.gateway, never()).loadSelectedCartItems(any(), any());
        verify(fixture.gateway, never()).loadBuyNowItem(any(), any(), anyInt());
    }

    private Fixture fixture() {
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutSessionItemRepository items = Mockito.mock(CheckoutSessionItemRepository.class);
        CheckoutSessionVoucherRepository sessionVouchers = Mockito.mock(CheckoutSessionVoucherRepository.class);
        CheckoutShippingQuoteRepository shippingQuotes = Mockito.mock(CheckoutShippingQuoteRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutSelectionGateway gateway = Mockito.mock(CheckoutSelectionGateway.class);
        CheckoutSessionService service = new CheckoutSessionService(
                sessions, items, sessionVouchers, shippingQuotes, sagas, gateway,
                new CheckoutProperties(Duration.ofMinutes(15)), Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, sessions, items, sessionVouchers, shippingQuotes, sagas, gateway);
    }

    private CheckoutSession activeSession() {
        return CheckoutSession.create(UUID.randomUUID(), CUSTOMER_ID, CheckoutSource.CART, CART_ID,
                "a".repeat(64), NOW.plus(Duration.ofMinutes(15)), NOW);
    }

    private TrustedCheckoutItem cartItem() {
        return new TrustedCheckoutItem(CART_ITEM_ID, 3L, PRODUCT_ID, VARIANT_ID, "SKU-1", "Product", "Blue", null,
                100_000, null, 10_000, 1, 200, 20, 10, 5);
    }

    private TrustedCheckoutItem buyNowItem(int quantity) {
        return new TrustedCheckoutItem(null, null, PRODUCT_ID, VARIANT_ID, "SKU-1", "Product", "Blue", null,
                100_000, null, 10_000, quantity, 200, 20, 10, 5);
    }

    private boolean captureOnlyItem(Iterable<CheckoutSessionItem> items, AtomicReference<CheckoutSessionItem> captured) {
        Iterator<CheckoutSessionItem> iterator = items.iterator();
        if (!iterator.hasNext()) {
            return false;
        }
        CheckoutSessionItem item = iterator.next();
        if (iterator.hasNext()) {
            return false;
        }
        captured.set(item);
        return true;
    }

    private record Fixture(
            CheckoutSessionService service,
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository items,
            CheckoutSessionVoucherRepository sessionVouchers,
            CheckoutShippingQuoteRepository shippingQuotes,
            OrderSagaRepository sagas,
            CheckoutSelectionGateway gateway) {
    }
}
