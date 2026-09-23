package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.ShippingQuoteResponse;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.client.VoucherPricingGateway.VoucherPreview;
import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.service.CheckoutPreviewPersistenceService.PersistedPreview;
import com.dynamicmart.order_service.service.CheckoutPreviewPersistenceService.PreviewCommit;
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

class CheckoutPreviewServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-23T09:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID MERCHANDISE_VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID SHIPPING_VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");

    @Test
    void previewsTrustedVouchersShippingAndMoneyOutsidePersistenceBoundary() {
        Fixture fixture = fixture();
        CheckoutSession session = session();
        session.setAddressId(ADDRESS_ID);
        CheckoutSessionItem item = item();
        when(fixture.sessions.findByIdAndCustomerId(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.items.findAllByCheckoutSessionId(SESSION_ID)).thenReturn(List.of(item));
        when(fixture.addresses.loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID)).thenReturn(address());
        when(fixture.vouchers.preview(any())).thenReturn(new VoucherPreview(
                List.of(
                        new AppliedVoucher(MERCHANDISE_VOUCHER_ID, "SALE20", "ORDER_DISCOUNT", 20_000, 0),
                        new AppliedVoucher(SHIPPING_VOUCHER_ID, "SHIP5", "SHIPPING_DISCOUNT", 0, 5_000)),
                List.of(new LineDiscount(VARIANT_ID, 0, 20_000))));
        when(fixture.payments.createShippingQuote(any())).thenReturn(validQuote());
        when(fixture.persistence.persist(any())).thenReturn(new PersistedPreview(PaymentTiming.PREPAID, PaymentMethod.VNPAY));

        var response = fixture.service.preview(CUSTOMER_ID, SESSION_ID,
                new CheckoutPreviewRequest(MERCHANDISE_VOUCHER_ID, SHIPPING_VOUCHER_ID, "standard"));

        assertEquals(180_000, response.money().itemsSubtotalVnd());
        assertEquals(20_000, response.money().orderDiscountVnd());
        assertEquals(185_000, response.money().finalTotalVnd());
        assertEquals(25_000, response.shipping().payableFeeVnd());
        ArgumentCaptor<PaymentClient.ShippingQuoteRequest> quoteRequest =
                ArgumentCaptor.forClass(PaymentClient.ShippingQuoteRequest.class);
        verify(fixture.payments).createShippingQuote(quoteRequest.capture());
        assertEquals(5_000, quoteRequest.getValue().shippingDiscountVnd());
        assertEquals(20, quoteRequest.getValue().items().get(0).lengthCm());
        ArgumentCaptor<PreviewCommit> commit = ArgumentCaptor.forClass(PreviewCommit.class);
        verify(fixture.persistence).persist(commit.capture());
        assertEquals(600, commit.getValue().packageMetrics().totalWeightGrams());
        assertEquals("a".repeat(64), commit.getValue().quote().requestFingerprint());
    }

    @Test
    void skipsVoucherServiceWhenNoVoucherWasSelected() {
        Fixture fixture = readyFixture();
        when(fixture.payments.createShippingQuote(any())).thenReturn(validQuoteWithoutDiscount());
        when(fixture.persistence.persist(any())).thenReturn(new PersistedPreview(null, null));

        fixture.service.preview(CUSTOMER_ID, SESSION_ID, new CheckoutPreviewRequest(null, null, null));

        verify(fixture.vouchers, never()).preview(any());
    }

    @Test
    void rejectsQuoteWithoutPortableFingerprintContract() {
        Fixture fixture = readyFixture();
        when(fixture.payments.createShippingQuote(any())).thenReturn(new ShippingQuoteResponse(
                QUOTE_ID, 30_000, 0, 30_000, 53320, "GHN Standard", "2-3 ngày",
                NOW.plus(Duration.ofMinutes(15)), null));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.preview(
                CUSTOMER_ID, SESSION_ID, new CheckoutPreviewRequest(null, null, null)));

        assertEquals("INVALID_SHIPPING_QUOTE_CONTRACT", exception.getCode());
        verify(fixture.persistence, never()).persist(any());
    }

    @Test
    void rejectsVoucherAllocationThatDoesNotMatchVoucherTotal() {
        Fixture fixture = readyFixture();
        when(fixture.vouchers.preview(any())).thenReturn(new VoucherPreview(
                List.of(new AppliedVoucher(MERCHANDISE_VOUCHER_ID, "SALE20", "ORDER_DISCOUNT", 20_000, 0)),
                List.of(new LineDiscount(VARIANT_ID, 0, 10_000))));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.preview(
                CUSTOMER_ID, SESSION_ID, new CheckoutPreviewRequest(MERCHANDISE_VOUCHER_ID, null, null)));

        assertEquals("INVALID_VOUCHER_PREVIEW", exception.getCode());
        verify(fixture.payments, never()).createShippingQuote(any());
    }

    private Fixture readyFixture() {
        Fixture fixture = fixture();
        CheckoutSession session = session();
        session.setAddressId(ADDRESS_ID);
        when(fixture.sessions.findByIdAndCustomerId(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(fixture.items.findAllByCheckoutSessionId(SESSION_ID)).thenReturn(List.of(item()));
        when(fixture.addresses.loadOwnedAddress(CUSTOMER_ID, ADDRESS_ID)).thenReturn(address());
        return fixture;
    }

    private Fixture fixture() {
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutSessionItemRepository items = Mockito.mock(CheckoutSessionItemRepository.class);
        AddressGateway addresses = Mockito.mock(AddressGateway.class);
        VoucherPricingGateway vouchers = Mockito.mock(VoucherPricingGateway.class);
        PaymentClient payments = Mockito.mock(PaymentClient.class);
        CheckoutPreviewPersistenceService persistence = Mockito.mock(CheckoutPreviewPersistenceService.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        CheckoutPreviewService service = new CheckoutPreviewService(
                sessions, items, addresses, vouchers, payments, new CheckoutPricingService(),
                new ShippingPackageCalculator(), persistence, clock);
        return new Fixture(service, sessions, items, addresses, vouchers, payments, persistence);
    }

    private CheckoutSession session() {
        return CheckoutSession.create(SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(),
                "b".repeat(64), NOW.plus(Duration.ofMinutes(15)), NOW);
    }

    private CheckoutSessionItem item() {
        return CheckoutSessionItem.create(UUID.randomUUID(), SESSION_ID, UUID.randomUUID(), 1L,
                PRODUCT_ID, VARIANT_ID, "SKU-1", "Product", "Blue", null,
                100_000, null, 10_000, 2, 300, 20, 10, 5, NOW);
    }

    private AddressSnapshot address() {
        return new AddressSnapshot(ADDRESS_ID, "Nguyễn Văn A", "0900000000", "Số 1",
                201, 301, "Hà Nội", "Phường A");
    }

    private ShippingQuoteResponse validQuote() {
        return new ShippingQuoteResponse(QUOTE_ID, 30_000, 5_000, 25_000, 53320,
                "GHN Standard", "2-3 ngày", NOW.plus(Duration.ofMinutes(15)), "a".repeat(64));
    }

    private ShippingQuoteResponse validQuoteWithoutDiscount() {
        return new ShippingQuoteResponse(QUOTE_ID, 30_000, 0, 30_000, 53320,
                "GHN Standard", "2-3 ngày", NOW.plus(Duration.ofMinutes(15)), "a".repeat(64));
    }

    private record Fixture(
            CheckoutPreviewService service,
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository items,
            AddressGateway addresses,
            VoucherPricingGateway vouchers,
            PaymentClient payments,
            CheckoutPreviewPersistenceService persistence) {
    }
}
