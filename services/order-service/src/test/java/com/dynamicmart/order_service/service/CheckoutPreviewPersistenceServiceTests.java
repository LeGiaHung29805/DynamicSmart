package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteResponse;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.CheckoutPreviewPersistenceService.PreviewCommit;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.ShippingPackageCalculator.PackageMetrics;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CheckoutPreviewPersistenceServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-23T09:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void invalidatesOldQuoteAndPersistsAuthoritativeTotals() {
        Fixture fixture = fixture();
        CheckoutSession session = session();
        CheckoutShippingQuote oldQuote = quote(UUID.randomUUID(), "c".repeat(64));
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.quotes.findByProviderAndQuoteId("GHN", QUOTE_ID)).thenReturn(Optional.empty());
        when(fixture.quotes.findAllByCheckoutSessionIdAndStatus(SESSION_ID, ShippingQuoteStatus.ACTIVE))
                .thenReturn(List.of(oldQuote));

        var result = fixture.service.persist(commit(pricing(185_000)));

        assertEquals(ShippingQuoteStatus.INVALIDATED, oldQuote.getStatus());
        assertEquals(185_000, session.getFinalTotalVnd());
        assertEquals(PaymentTiming.PREPAID, result.paymentTiming());
        ArgumentCaptor<CheckoutShippingQuote> storedQuote = ArgumentCaptor.forClass(CheckoutShippingQuote.class);
        verify(fixture.quotes).save(storedQuote.capture());
        assertEquals(QUOTE_ID, storedQuote.getValue().getQuoteId());
        assertEquals("a".repeat(64), storedQuote.getValue().getInputFingerprint());
    }

    @Test
    void zeroValuePreviewForcesBackendOwnedFreePaymentPair() {
        Fixture fixture = fixture();
        CheckoutSession session = session();
        session.setPaymentTiming(PaymentTiming.POSTPAID);
        session.setPaymentMethod(PaymentMethod.COD);
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.quotes.findByProviderAndQuoteId("GHN", QUOTE_ID)).thenReturn(Optional.empty());
        when(fixture.quotes.findAllByCheckoutSessionIdAndStatus(SESSION_ID, ShippingQuoteStatus.ACTIVE))
                .thenReturn(List.of());

        var result = fixture.service.persist(commit(new PricingBreakdown(0, 0, 0, 0, 0, 0, 0, 0)));

        assertEquals(PaymentTiming.NOT_REQUIRED, result.paymentTiming());
        assertEquals(PaymentMethod.FREE, result.paymentMethod());
        assertEquals(PaymentMethod.FREE, session.getPaymentMethod());
    }

    @Test
    void staleSelectionIsRejectedBeforeReplacingQuoteOrVoucher() {
        Fixture fixture = fixture();
        CheckoutSession session = session();
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        PreviewCommit stale = new PreviewCommit(
                CUSTOMER_ID, SESSION_ID, "f".repeat(64), address(), List.of(), pricing(185_000),
                paymentQuote(), new PackageMetrics(600, 20, 10, 10));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.persist(stale));

        assertEquals("CHECKOUT_PREVIEW_STALE", exception.getCode());
        verify(fixture.quotes, never()).save(any());
        verify(fixture.vouchers, never()).deleteAllByCheckoutSessionId(any());
    }

    @Test
    void previewIsRejectedAfterOrderCreationAdmission() {
        Fixture fixture = fixture();
        when(fixture.sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session()));
        when(fixture.sagas.existsByCheckoutSessionId(SESSION_ID)).thenReturn(true);

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.persist(commit(pricing(185_000))));

        assertEquals("ORDER_CREATION_IN_PROGRESS", exception.getCode());
        verify(fixture.quotes, never()).save(any());
        verify(fixture.vouchers, never()).deleteAllByCheckoutSessionId(any());
    }

    private Fixture fixture() {
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutSessionVoucherRepository vouchers = Mockito.mock(CheckoutSessionVoucherRepository.class);
        CheckoutShippingQuoteRepository quotes = Mockito.mock(CheckoutShippingQuoteRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutPreviewPersistenceService service = new CheckoutPreviewPersistenceService(
                sessions, vouchers, quotes, sagas, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, sessions, vouchers, quotes, sagas);
    }

    private CheckoutSession session() {
        CheckoutSession session = CheckoutSession.create(SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(),
                "b".repeat(64), NOW.plus(Duration.ofMinutes(15)), NOW);
        session.setAddressId(ADDRESS_ID);
        session.setPaymentTiming(PaymentTiming.PREPAID);
        session.setPaymentMethod(PaymentMethod.VNPAY);
        return session;
    }

    private PreviewCommit commit(PricingBreakdown pricing) {
        return new PreviewCommit(CUSTOMER_ID, SESSION_ID, "b".repeat(64), address(), List.of(), pricing,
                paymentQuote(), new PackageMetrics(600, 20, 10, 10));
    }

    private AddressSnapshot address() {
        return new AddressSnapshot(ADDRESS_ID, "Nguyễn Văn A", "0900000000", "Số 1",
                201, 301, "Hà Nội", "Phường A");
    }

    private PricingBreakdown pricing(long finalTotal) {
        return new PricingBreakdown(200_000, 20_000, 180_000, 0, 20_000, 30_000, 5_000, finalTotal);
    }

    private ShippingQuoteResponse paymentQuote() {
        return new ShippingQuoteResponse(QUOTE_ID, 30_000, 5_000, 25_000, 53320, "GHN Standard", "2-3 ngày",
                NOW.plus(Duration.ofMinutes(15)), "a".repeat(64));
    }

    private CheckoutShippingQuote quote(UUID quoteId, String fingerprint) {
        return CheckoutShippingQuote.create(UUID.randomUUID(), SESSION_ID, quoteId, fingerprint,
                30_000, 0, 30_000, 53320, "GHN", "2-3 ngày", 600, 20, 10, 10,
                201, 301, "Hà Nội", "Phường A", NOW.plus(Duration.ofMinutes(15)), NOW.minusSeconds(10));
    }

    private record Fixture(
            CheckoutPreviewPersistenceService service,
            CheckoutSessionRepository sessions,
            CheckoutSessionVoucherRepository vouchers,
            CheckoutShippingQuoteRepository quotes,
            OrderSagaRepository sagas) {
    }
}
