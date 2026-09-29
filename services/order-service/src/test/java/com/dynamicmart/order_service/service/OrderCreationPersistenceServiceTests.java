package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.AddressGateway.AddressSnapshot;
import com.dynamicmart.order_service.client.VoucherPricingGateway.AppliedVoucher;
import com.dynamicmart.order_service.client.VoucherPricingGateway.LineDiscount;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderItem;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderAddressRepository;
import com.dynamicmart.order_service.repository.OrderItemRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OrderShippingSnapshotRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OrderVoucherSnapshotRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationContextReader.ItemSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.MoneySnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.QuoteSnapshot;
import com.dynamicmart.order_service.service.OrderCreationContextReader.VoucherSnapshot;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import com.dynamicmart.order_service.service.OrderReservationService.ReservationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

class OrderCreationPersistenceServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID CHECKOUT_QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID PROVIDER_QUOTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID VOUCHER_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");
    private static final UUID INVENTORY_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000008");
    private static final UUID VARIANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");

    @Test
    void atomicallyCreatesOrderAndEveryImmutableSnapshot() {
        Fixture fixture = readyFixture(PaymentTiming.PREPAID, PaymentMethod.VNPAY);

        var result = fixture.service.persist(
                input(PaymentTiming.PREPAID, PaymentMethod.VNPAY), reservations());

        assertFalse(result.replay());
        assertEquals(OrderStatus.PENDING_PAYMENT, result.status());
        assertEquals(CheckoutStatus.COMPLETED, fixture.session.getStatus());
        assertEquals(result.orderId(), fixture.session.getCompletedOrderId());
        assertEquals(ShippingQuoteStatus.CONSUMED, fixture.quote.getStatus());
        assertEquals(NOW, fixture.quote.getConsumedAt());
        assertEquals(SagaStatus.ORDER_CREATED, fixture.saga.getStatus());
        assertEquals(result.orderId(), fixture.saga.getOrderId());

        ArgumentCaptor<CustomerOrder> order = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(fixture.orders).save(order.capture());
        assertEquals("VND", order.getValue().getCurrency());
        assertEquals(210_000, order.getValue().getFinalTotalVnd());
        assertTrue(order.getValue().getOrderNumber().startsWith("ORD-"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItem>> items = ArgumentCaptor.forClass(List.class);
        verify(fixture.orderItems).saveAll(items.capture());
        assertEquals(190_000, items.getValue().get(0).getLineTotalVnd());
        verify(fixture.orderAddresses).save(any());
        verify(fixture.orderVouchers).saveAll(any());
        ArgumentCaptor<OrderShippingSnapshot> shipping = ArgumentCaptor.forClass(OrderShippingSnapshot.class);
        verify(fixture.orderShipping).save(shipping.capture());
        assertEquals(CHECKOUT_QUOTE_ID, shipping.getValue().getSourceCheckoutQuoteId());
        assertEquals(PROVIDER_QUOTE_ID, shipping.getValue().getQuoteId());
        verify(fixture.histories).save(any());
        verify(fixture.outbox).save(any());
    }

    @Test
    void postpaidOrderStartsConfirmed() {
        Fixture fixture = readyFixture(PaymentTiming.POSTPAID, PaymentMethod.COD);

        var result = fixture.service.persist(
                input(PaymentTiming.POSTPAID, PaymentMethod.COD), reservations());

        assertEquals(OrderStatus.CONFIRMED, result.status());
    }

    @Test
    void committedOrderIsReplayedWithoutWritingSnapshotsAgain() {
        Fixture fixture = readyFixture(PaymentTiming.PREPAID, PaymentMethod.VNPAY);
        UUID orderId = UUID.randomUUID();
        CustomerOrder existing = CustomerOrder.create(
                orderId, "ORD-EXISTING", SESSION_ID, CUSTOMER_ID, OrderStatus.PENDING_PAYMENT,
                PaymentTiming.PREPAID, PaymentMethod.VNPAY, 200_000, 0, 200_000, 0, 10_000,
                20_000, 0, 210_000, null, NOW.minusSeconds(10));
        fixture.session.complete(orderId, NOW.minusSeconds(10));
        fixture.saga.markOrderCreated(orderId, NOW.minusSeconds(10));
        when(fixture.orders.findByCheckoutSessionId(SESSION_ID)).thenReturn(Optional.of(existing));

        var result = fixture.service.persist(
                input(PaymentTiming.PREPAID, PaymentMethod.VNPAY), reservations());

        assertTrue(result.replay());
        assertEquals(orderId, result.orderId());
        verify(fixture.quotes, never()).findByIdForUpdate(any());
        verify(fixture.orderItems, never()).saveAll(any());
        verify(fixture.outbox, never()).save(any());
    }

    @Test
    void rejectsReservationThatDoesNotMatchDurableSagaCheckpoint() {
        Fixture fixture = readyFixture(PaymentTiming.PREPAID, PaymentMethod.VNPAY);
        ReservationResult wrong = new ReservationResult(VOUCHER_RESERVATION_ID, UUID.randomUUID());

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.persist(
                input(PaymentTiming.PREPAID, PaymentMethod.VNPAY), wrong));

        assertEquals("RESERVATION_CHECKPOINT_MISMATCH", exception.getCode());
        verify(fixture.orders, never()).save(any());
    }

    @Test
    void rejectsQuoteChangedAfterRevalidation() {
        Fixture fixture = readyFixture(PaymentTiming.PREPAID, PaymentMethod.VNPAY);
        fixture.quote.setStatus(ShippingQuoteStatus.INVALIDATED);

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.persist(
                input(PaymentTiming.PREPAID, PaymentMethod.VNPAY), reservations()));

        assertEquals("SHIPPING_QUOTE_STALE", exception.getCode());
        verify(fixture.orders, never()).save(any());
    }

    private Fixture readyFixture(PaymentTiming timing, PaymentMethod method) {
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutShippingQuoteRepository quotes = Mockito.mock(CheckoutShippingQuoteRepository.class);
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderItemRepository orderItems = Mockito.mock(OrderItemRepository.class);
        OrderAddressRepository orderAddresses = Mockito.mock(OrderAddressRepository.class);
        OrderVoucherSnapshotRepository orderVouchers = Mockito.mock(OrderVoucherSnapshotRepository.class);
        OrderShippingSnapshotRepository orderShipping = Mockito.mock(OrderShippingSnapshotRepository.class);
        OrderStatusHistoryRepository histories = Mockito.mock(OrderStatusHistoryRepository.class);
        OutboxEventRepository outbox = Mockito.mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);

        CheckoutSession session = session(timing, method);
        OrderSaga saga = saga();
        CheckoutShippingQuote quote = quote();
        when(sessions.findOwnedForUpdate(SESSION_ID, CUSTOMER_ID)).thenReturn(Optional.of(session));
        when(sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));
        when(orders.findByCheckoutSessionId(SESSION_ID)).thenReturn(Optional.empty());
        when(quotes.findByIdForUpdate(CHECKOUT_QUOTE_ID)).thenReturn(Optional.of(quote));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"type\":\"OrderCreated\"}");

        OrderCreationPersistenceService service = new OrderCreationPersistenceService(
                sessions, sagas, quotes, orders, orderItems, orderAddresses, orderVouchers, orderShipping,
                histories, outbox, new OrderStateMachine(), objectMapper, Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(
                service, session, saga, quote, quotes, orders, orderItems, orderAddresses, orderVouchers,
                orderShipping, histories, outbox);
    }

    private CheckoutSession session(PaymentTiming timing, PaymentMethod method) {
        CheckoutSession session = CheckoutSession.create(
                SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(), "b".repeat(64),
                NOW.plusSeconds(600), NOW.minusSeconds(60));
        session.setAddressId(UUID.randomUUID());
        session.setPaymentTiming(timing);
        session.setPaymentMethod(method);
        return session;
    }

    private OrderSaga saga() {
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, SESSION_ID, UUID.randomUUID(), "c".repeat(64), CORRELATION_ID, NOW.minusSeconds(30));
        saga.setStatus(SagaStatus.INVENTORY_RESERVED);
        saga.setVoucherReservationId(VOUCHER_RESERVATION_ID);
        saga.setInventoryReservationId(INVENTORY_RESERVATION_ID);
        return saga;
    }

    private CheckoutShippingQuote quote() {
        CheckoutShippingQuote quote = CheckoutShippingQuote.create(
                CHECKOUT_QUOTE_ID, SESSION_ID, PROVIDER_QUOTE_ID, "a".repeat(64),
                20_000, 0, 20_000, 1, "GHN Standard", "1 ngày", 400, 20, 10, 10,
                1, 2, "Hà Nội", "Phường A", NOW.plusSeconds(300), NOW.minusSeconds(60));
        quote.setRawResponseRedacted("{}");
        return quote;
    }

    private ValidatedOrderInput input(PaymentTiming timing, PaymentMethod method) {
        UUID productId = UUID.randomUUID();
        UUID voucherId = UUID.randomUUID();
        ItemSnapshot item = new ItemSnapshot(
                UUID.randomUUID(), 1L, productId, VARIANT_ID, "SKU-1", "Sản phẩm", "Đỏ", null,
                100_000, null, 0, 100_000, 2, 200, 20, 10, 5);
        CreationContext context = new CreationContext(
                SAGA_ID, CORRELATION_ID, SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(),
                UUID.randomUUID(), timing, method,
                new MoneySnapshot(200_000, 0, 200_000, 0, 10_000, 20_000, 0, 210_000),
                List.of(item),
                List.of(new VoucherSnapshot(voucherId, "SAVE10", "ORDER_DISCOUNT", 10_000, 0)),
                new QuoteSnapshot(
                        CHECKOUT_QUOTE_ID, PROVIDER_QUOTE_ID, "GHN", "a".repeat(64),
                        20_000, 0, 20_000, 1, "GHN Standard", null, "1 ngày",
                        400, 20, 10, 10, 1, 2, "Hà Nội", "Phường A", NOW.plusSeconds(300)));
        return new ValidatedOrderInput(
                context,
                new AddressSnapshot(context.addressId(), "Hiếu", "0900000000", "1 Đường A",
                        1, 2, "Hà Nội", "Phường A"),
                List.of(new AppliedVoucher(
                        voucherId, "SAVE10", "ORDER_DISCOUNT", "FIXED_AMOUNT", 10_000L,
                        200_000, 10_000, 0)),
                List.of(new LineDiscount(VARIANT_ID, 0, 10_000)),
                new PricingBreakdown(200_000, 0, 200_000, 0, 10_000, 20_000, 0, 210_000));
    }

    private ReservationResult reservations() {
        return new ReservationResult(VOUCHER_RESERVATION_ID, INVENTORY_RESERVATION_ID);
    }

    private record Fixture(
            OrderCreationPersistenceService service,
            CheckoutSession session,
            OrderSaga saga,
            CheckoutShippingQuote quote,
            CheckoutShippingQuoteRepository quotes,
            CustomerOrderRepository orders,
            OrderItemRepository orderItems,
            OrderAddressRepository orderAddresses,
            OrderVoucherSnapshotRepository orderVouchers,
            OrderShippingSnapshotRepository orderShipping,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox) {
    }
}
