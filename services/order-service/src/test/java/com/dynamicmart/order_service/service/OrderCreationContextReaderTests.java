package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderCreationContextReaderTests {
    private static final Instant NOW = Instant.parse("2026-09-24T02:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void readsImmutableCreationContextInOneDatabaseBoundary() {
        Fixture fixture = fixture();
        CheckoutSession session = readySession();
        OrderSaga saga = saga();
        when(fixture.sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga));
        when(fixture.sessions.findByIdAndCustomerId(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.quotes.findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                SESSION_ID, ShippingQuoteStatus.ACTIVE, NOW)).thenReturn(Optional.of(quote()));
        when(fixture.items.findAllByCheckoutSessionId(SESSION_ID)).thenReturn(List.of(item()));
        when(fixture.vouchers.findAllByCheckoutSessionId(SESSION_ID)).thenReturn(List.of(voucher()));

        var context = fixture.reader.load(CUSTOMER_ID, SAGA_ID);

        assertEquals(SESSION_ID, context.checkoutSessionId());
        assertEquals(185_000, context.money().finalTotalVnd());
        assertEquals(1, context.items().size());
        assertEquals(1, context.vouchers().size());
        assertEquals("GHN", context.quote().provider());
        assertEquals("b".repeat(64), context.quote().inputFingerprint());
    }

    @Test
    void rejectsSagaThatAlreadyPassedRevalidationBoundary() {
        Fixture fixture = fixture();
        OrderSaga saga = saga();
        saga.setStatus(SagaStatus.VOUCHER_RESERVED);
        when(fixture.sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga));
        when(fixture.sessions.findByIdAndCustomerId(SESSION_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(readySession()));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.reader.load(CUSTOMER_ID, SAGA_ID));

        assertEquals("ORDER_SAGA_NOT_REVALIDATABLE", exception.getCode());
    }

    @Test
    void rejectsLegacySagaWhenPreviewSnapshotIsIncomplete() {
        Fixture fixture = fixture();
        CheckoutSession session = readySession();
        session.setFinalTotalVnd(null);
        when(fixture.sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga()));
        when(fixture.sessions.findByIdAndCustomerId(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.reader.load(CUSTOMER_ID, SAGA_ID));

        assertEquals("CHECKOUT_PREVIEW_REQUIRED", exception.getCode());
    }

    private Fixture fixture() {
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutSessionItemRepository items = Mockito.mock(CheckoutSessionItemRepository.class);
        CheckoutSessionVoucherRepository vouchers = Mockito.mock(CheckoutSessionVoucherRepository.class);
        CheckoutShippingQuoteRepository quotes = Mockito.mock(CheckoutShippingQuoteRepository.class);
        return new Fixture(sagas, sessions, items, vouchers, quotes,
                new OrderCreationContextReader(
                        sagas, sessions, items, vouchers, quotes, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private OrderSaga saga() {
        return OrderSaga.start(
                SAGA_ID, SESSION_ID, UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), NOW.minusSeconds(5));
    }

    private CheckoutSession readySession() {
        CheckoutSession session = CheckoutSession.create(
                SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(), "a".repeat(64),
                NOW.plusSeconds(900), NOW.minusSeconds(60));
        session.setAddressId(UUID.randomUUID());
        session.setPaymentTiming(PaymentTiming.PREPAID);
        session.setPaymentMethod(PaymentMethod.VNPAY);
        session.setItemsListSubtotalVnd(200_000L);
        session.setDirectSaleDiscountVnd(20_000L);
        session.setItemsSubtotalVnd(180_000L);
        session.setProductDiscountVnd(10_000L);
        session.setOrderDiscountVnd(5_000L);
        session.setShippingFeeVnd(30_000L);
        session.setShippingDiscountVnd(10_000L);
        session.setFinalTotalVnd(185_000L);
        return session;
    }

    private CheckoutSessionItem item() {
        return CheckoutSessionItem.create(
                UUID.randomUUID(), SESSION_ID, UUID.randomUUID(), 2L, UUID.randomUUID(), UUID.randomUUID(),
                "SKU-1", "Sản phẩm", "Đỏ", null, 100_000, UUID.randomUUID(), 10_000,
                2, 200, 20, 10, 5, NOW.minusSeconds(60));
    }

    private CheckoutSessionVoucher voucher() {
        return CheckoutSessionVoucher.create(
                UUID.randomUUID(), SESSION_ID, UUID.randomUUID(), "SAVE15", "ORDER_DISCOUNT",
                15_000, 0, NOW.minusSeconds(30));
    }

    private CheckoutShippingQuote quote() {
        return CheckoutShippingQuote.create(
                UUID.randomUUID(), SESSION_ID, UUID.randomUUID(), "b".repeat(64),
                30_000, 10_000, 20_000, 53321, "GHN Express", "1-2 ngày",
                400, 20, 10, 10, 1, 2, "Hà Nội", "Phường A", NOW.plusSeconds(600), NOW.minusSeconds(10));
    }

    private record Fixture(
            OrderSagaRepository sagas,
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository items,
            CheckoutSessionVoucherRepository vouchers,
            CheckoutShippingQuoteRepository quotes,
            OrderCreationContextReader reader) {
    }
}
