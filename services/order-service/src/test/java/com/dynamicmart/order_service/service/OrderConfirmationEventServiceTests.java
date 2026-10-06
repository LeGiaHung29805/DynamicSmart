package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderOperationLog;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderOperationLogRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
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

class OrderConfirmationEventServiceTests {
    private static final UUID ORDER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID CART_ITEM_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID SAGA_ID = UUID.fromString("30000000-0000-0000-0000-000000000005");
    private static final UUID CORRELATION_ID = UUID.fromString("30000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-10-02T03:00:00Z");

    @Test
    void cartOrderEnqueuesExactCleanupSnapshotOnce() {
        Fixture fixture = fixture(CheckoutSource.CART, SagaStatus.INVENTORY_COMMITTED, cartItem(7L));

        var result = fixture.service.enqueueIfEligible(ORDER_ID);

        assertFalse(result.replay());
        assertFalse(result.skippedBuyNow());
        ArgumentCaptor<com.dynamicmart.order_service.entity.OutboxEvent> event =
                ArgumentCaptor.forClass(com.dynamicmart.order_service.entity.OutboxEvent.class);
        verify(fixture.outbox).save(event.capture());
        assertEquals("OrderConfirmed", event.getValue().getEventType());
        assertEquals(CORRELATION_ID, event.getValue().getCorrelationId());
        assertTrue(event.getValue().getPayload().contains(CART_ITEM_ID.toString()));
        assertTrue(event.getValue().getPayload().contains("\"version\":7"));
        verify(fixture.operationLogs).save(any());
    }

    @Test
    void existingCheckpointReplaysSameEventWithoutDuplicateOutbox() {
        Fixture fixture = fixture(CheckoutSource.CART, SagaStatus.COMPLETED, cartItem(7L));
        UUID eventId = UUID.randomUUID();
        UUID key = OrderConfirmationEventService.operationKey(ORDER_ID);
        when(fixture.operationLogs.findById(key)).thenReturn(Optional.of(OrderOperationLog.completed(
                key, "ENQUEUE_ORDER_CONFIRMED", ORDER_ID,
                "{\"eventId\":\"" + eventId + "\"}", NOW.minusSeconds(1))));

        var result = fixture.service.enqueueIfEligible(ORDER_ID);

        assertTrue(result.replay());
        assertEquals(eventId, result.eventId());
        verify(fixture.outbox, never()).save(any());
    }

    @Test
    void buyNowNeverCreatesCartCleanupEvent() {
        Fixture fixture = fixture(CheckoutSource.BUY_NOW, SagaStatus.COMPLETED, cartItem(null));

        var result = fixture.service.enqueueIfEligible(ORDER_ID);

        assertTrue(result.skippedBuyNow());
        verify(fixture.outbox, never()).save(any());
        verify(fixture.operationLogs, never()).save(any());
    }

    @Test
    void cartSnapshotWithoutVersionIsRejected() {
        Fixture fixture = fixture(CheckoutSource.CART, SagaStatus.INVENTORY_COMMITTED, cartItem(null));

        OrderException error = assertThrows(
                OrderException.class, () -> fixture.service.enqueueIfEligible(ORDER_ID));

        assertEquals("ORDER_CONFIRMED_ITEM_CONTRACT_INVALID", error.getCode());
        verify(fixture.outbox, never()).save(any());
    }

    private Fixture fixture(CheckoutSource source, SagaStatus sagaStatus, CheckoutSessionItem item) {
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        CheckoutSessionItemRepository items = Mockito.mock(CheckoutSessionItemRepository.class);
        OrderOperationLogRepository operationLogs = Mockito.mock(OrderOperationLogRepository.class);
        OutboxEventRepository outbox = Mockito.mock(OutboxEventRepository.class);
        CustomerOrder order = CustomerOrder.create(
                ORDER_ID, "ORD-CONFIRMED", SESSION_ID, CUSTOMER_ID, OrderStatus.CONFIRMED,
                PaymentTiming.POSTPAID, PaymentMethod.COD, 100, 0, 100, 0, 0, 0, 0, 100,
                null, NOW.minusSeconds(30));
        CheckoutSession session = CheckoutSession.create(
                SESSION_ID, CUSTOMER_ID, source, source == CheckoutSource.CART ? UUID.randomUUID() : null,
                "a".repeat(64), NOW.plusSeconds(60), NOW.minusSeconds(60));
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, SESSION_ID, UUID.randomUUID(), "b".repeat(64), CORRELATION_ID,
                NOW.minusSeconds(60));
        saga.setOrderId(ORDER_ID);
        saga.setStatus(sagaStatus);
        when(orders.findForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(sessions.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(sagas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(saga));
        when(items.findAllByCheckoutSessionId(SESSION_ID)).thenReturn(List.of(item));
        when(operationLogs.findById(OrderConfirmationEventService.operationKey(ORDER_ID)))
                .thenReturn(Optional.empty());
        OrderConfirmationEventService service = new OrderConfirmationEventService(
                orders, sagas, sessions, items, operationLogs, outbox, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, operationLogs, outbox);
    }

    private CheckoutSessionItem cartItem(Long version) {
        CheckoutSessionItem item = CheckoutSessionItem.create(
                UUID.randomUUID(), SESSION_ID, CART_ITEM_ID, version, UUID.randomUUID(), UUID.randomUUID(),
                "SKU", "Product", "Variant", null, 100, null, 0, 2,
                100, 10, 10, 10, NOW.minusSeconds(60));
        return item;
    }

    private record Fixture(
            OrderConfirmationEventService service,
            OrderOperationLogRepository operationLogs,
            OutboxEventRepository outbox) {
    }
}
