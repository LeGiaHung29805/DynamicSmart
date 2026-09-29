package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OrderCreationAdmissionServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-24T02:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void startsDurableSagaOnlyAfterCheckoutAndQuoteAreReady() {
        Fixture fixture = readyFixture();

        var result = fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);

        assertFalse(result.replay());
        assertNotNull(result.sagaId());
        assertNotNull(result.correlationId());
        ArgumentCaptor<OrderSaga> saga = ArgumentCaptor.forClass(OrderSaga.class);
        verify(fixture.sagas).save(saga.capture());
        assertEquals(SESSION_ID, saga.getValue().getCheckoutSessionId());
        assertEquals(IDEMPOTENCY_KEY, saga.getValue().getIdempotencyKey());
        assertEquals(64, saga.getValue().getRequestHash().length());
        assertEquals("ADMITTED", saga.getValue().getCurrentStep());
    }

    @Test
    void sameKeyAndRequestReplaysExistingSagaWithoutCreatingAnother() {
        Fixture fixture = readyFixture();
        var first = fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);
        ArgumentCaptor<OrderSaga> saved = ArgumentCaptor.forClass(OrderSaga.class);
        verify(fixture.sagas).save(saved.capture());
        when(fixture.sagas.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(saved.getValue()));

        var replay = fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);

        assertTrue(replay.replay());
        assertEquals(first.sagaId(), replay.sagaId());
        assertEquals(first.correlationId(), replay.correlationId());
        verify(fixture.sagas, times(1)).save(any());
    }

    @Test
    void rejectsIdempotencyKeyReusedForAnotherRequest() {
        Fixture fixture = readyFixture();
        OrderSaga otherRequest = OrderSaga.start(
                UUID.randomUUID(), UUID.randomUUID(), IDEMPOTENCY_KEY, "f".repeat(64),
                UUID.randomUUID(), NOW);
        when(fixture.sagas.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(otherRequest));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.getCode());
        verify(fixture.sagas, never()).save(any());
    }

    @Test
    void rejectsDifferentKeyAfterCreationAlreadyStartedForSession() {
        Fixture fixture = readyFixture();
        OrderSaga existing = OrderSaga.start(
                UUID.randomUUID(), SESSION_ID, UUID.randomUUID(), "a".repeat(64),
                UUID.randomUUID(), NOW);
        when(fixture.sagas.findByCheckoutSessionId(SESSION_ID)).thenReturn(Optional.of(existing));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals("ORDER_CREATION_ALREADY_STARTED", exception.getCode());
        verify(fixture.sagas, never()).save(any());
    }

    @Test
    void expiresStaleSessionBeforeAnySagaIsCreated() {
        Fixture fixture = fixture();
        CheckoutSession session = readySession();
        session.setExpiresAt(NOW);
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals("CHECKOUT_SESSION_EXPIRED", exception.getCode());
        assertEquals(CheckoutStatus.EXPIRED, session.getStatus());
        verify(fixture.quotes, never())
                .findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(any(), any(), any());
        verify(fixture.sagas, never()).save(any());
    }

    @Test
    void requiresAuthoritativePreviewTotals() {
        Fixture fixture = fixture();
        CheckoutSession session = readySession();
        session.setFinalTotalVnd(null);
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals("CHECKOUT_PREVIEW_REQUIRED", exception.getCode());
        verify(fixture.sagas, never()).save(any());
    }

    @Test
    void requiresAnActiveUnexpiredShippingQuote() {
        Fixture fixture = fixture();
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(readySession()));
        when(fixture.quotes.findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                SESSION_ID, ShippingQuoteStatus.ACTIVE, NOW)).thenReturn(Optional.empty());

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals("ACTIVE_SHIPPING_QUOTE_REQUIRED", exception.getCode());
        verify(fixture.sagas, never()).save(any());
    }

    private Fixture readyFixture() {
        Fixture fixture = fixture();
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(readySession()));
        when(fixture.quotes.findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                SESSION_ID, ShippingQuoteStatus.ACTIVE, NOW)).thenReturn(Optional.of(quote()));
        return fixture;
    }

    private Fixture fixture() {
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutShippingQuoteRepository quotes = Mockito.mock(CheckoutShippingQuoteRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new Fixture(sessions, quotes, sagas,
                new OrderCreationAdmissionService(sessions, quotes, sagas, clock));
    }

    private CheckoutSession readySession() {
        CheckoutSession session = CheckoutSession.create(
                SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(), "b".repeat(64),
                NOW.plusSeconds(900), NOW.minusSeconds(60));
        session.setAddressId(UUID.randomUUID());
        session.setPaymentTiming(PaymentTiming.PREPAID);
        session.setPaymentMethod(PaymentMethod.VNPAY);
        session.setItemsListSubtotalVnd(120_000L);
        session.setDirectSaleDiscountVnd(20_000L);
        session.setItemsSubtotalVnd(100_000L);
        session.setProductDiscountVnd(5_000L);
        session.setOrderDiscountVnd(0L);
        session.setShippingFeeVnd(30_000L);
        session.setShippingDiscountVnd(10_000L);
        session.setFinalTotalVnd(115_000L);
        return session;
    }

    private CheckoutShippingQuote quote() {
        return CheckoutShippingQuote.create(
                UUID.randomUUID(), SESSION_ID, QUOTE_ID, "c".repeat(64),
                30_000, 10_000, 20_000, 53321, "GHN Express", "1-2 ngày",
                1_000, 20, 15, 10, 1, 2, "Hà Nội", "Phường A",
                NOW.plusSeconds(600), NOW.minusSeconds(10));
    }

    private record Fixture(
            CheckoutSessionRepository sessions,
            CheckoutShippingQuoteRepository quotes,
            OrderSagaRepository sagas,
            OrderCreationAdmissionService service) {
    }
}
