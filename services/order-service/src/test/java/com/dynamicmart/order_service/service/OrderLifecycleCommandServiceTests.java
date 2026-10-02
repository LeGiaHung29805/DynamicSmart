package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.dto.response.OrderCommandResponse;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderOperationLog;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderOperationLogRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
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

class OrderLifecycleCommandServiceTests {
    private static final UUID ORDER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID OPERATION_KEY = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-02T02:00:00Z");

    @Test
    void adminPackUsesStateMachineAndWritesTimeline() {
        Fixture fixture = fixture(order(OrderStatus.CONFIRMED, PaymentTiming.POSTPAID, PaymentMethod.COD));

        var result = fixture.service.adminPack(ADMIN_ID, ORDER_ID, OPERATION_KEY);

        assertEquals(OrderStatus.PACKING, result.status());
        assertEquals(List.of(OrderStatus.PACKING), result.appliedTransitions());
        assertFalse(result.replay());
        verify(fixture.histories).save(any());
        verify(fixture.operationLogs).save(any());
        verify(fixture.outbox, never()).save(any());
    }

    @Test
    void postpaidVnPayHandoverEmitsPaymentDue() {
        Fixture fixture = fixture(order(OrderStatus.SHIPPING, PaymentTiming.POSTPAID, PaymentMethod.VNPAY));

        fixture.service.adminHandover(ADMIN_ID, ORDER_ID, OPERATION_KEY);

        assertEquals(OrderStatus.HANDOVER_PENDING, fixture.order.getStatus());
        ArgumentCaptor<com.dynamicmart.order_service.entity.OutboxEvent> event =
                ArgumentCaptor.forClass(com.dynamicmart.order_service.entity.OutboxEvent.class);
        verify(fixture.outbox).save(event.capture());
        assertEquals("PaymentDue", event.getValue().getEventType());
        assertEquals(OPERATION_KEY, event.getValue().getCorrelationId());
    }

    @Test
    void unpaidPostpaidReceiptStopsAtDelivered() {
        Fixture fixture = fixture(order(
                OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, PaymentMethod.COD));

        var result = fixture.service.customerConfirmReceived(CUSTOMER_ID, ORDER_ID, OPERATION_KEY);

        assertEquals(OrderStatus.DELIVERED, result.status());
        assertEquals(List.of(OrderStatus.DELIVERED), result.appliedTransitions());
        assertEquals(NOW, fixture.order.getShipmentDeliveredAt());
        ArgumentCaptor<com.dynamicmart.order_service.entity.OutboxEvent> event =
                ArgumentCaptor.forClass(com.dynamicmart.order_service.entity.OutboxEvent.class);
        verify(fixture.outbox).save(event.capture());
        assertEquals("ShipmentDelivered", event.getValue().getEventType());
    }

    @Test
    void paidPostpaidReceiptDeliversAndCompletesInOneTransaction() {
        CustomerOrder order = order(OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, PaymentMethod.COD);
        order.setPaymentSucceededAt(NOW.minusSeconds(30));
        Fixture fixture = fixture(order);

        var result = fixture.service.customerConfirmReceived(CUSTOMER_ID, ORDER_ID, OPERATION_KEY);

        assertEquals(OrderStatus.COMPLETED, result.status());
        assertEquals(List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED), result.appliedTransitions());
        assertEquals(NOW, fixture.order.getCompletedAt());
        verify(fixture.histories, times(2)).save(any());
        verify(fixture.outbox, times(2)).save(any());
    }

    @Test
    void customerCannotConfirmAnotherCustomersOrder() {
        Fixture fixture = fixture(order(
                OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, PaymentMethod.COD));
        UUID anotherCustomer = UUID.randomUUID();
        when(fixture.orders.findOwnedForUpdate(ORDER_ID, anotherCustomer)).thenReturn(Optional.empty());

        OrderException error = assertThrows(
                OrderException.class,
                () -> fixture.service.customerConfirmReceived(anotherCustomer, ORDER_ID, OPERATION_KEY));

        assertEquals("ORDER_NOT_FOUND", error.getCode());
        assertEquals(404, error.getStatus().value());
        verify(fixture.histories, never()).save(any());
    }

    @Test
    void sameOperationKeyReplaysStoredResponseWithoutNewSideEffects() {
        Fixture fixture = fixture(order(OrderStatus.PACKING, PaymentTiming.POSTPAID, PaymentMethod.COD));
        OrderCommandResponse stored = new OrderCommandResponse(
                ORDER_ID, OrderStatus.PACKING, List.of(OrderStatus.PACKING), NOW.minusSeconds(10), false);
        when(fixture.operationLogs.findById(OPERATION_KEY)).thenReturn(Optional.of(OrderOperationLog.completed(
                OPERATION_KEY, "ADMIN_PACK", ORDER_ID,
                fixture.objectMapper.writeValueAsString(stored), NOW.minusSeconds(10))));

        var result = fixture.service.adminPack(ADMIN_ID, ORDER_ID, OPERATION_KEY);

        assertTrue(result.replay());
        assertEquals(stored.status(), result.status());
        verify(fixture.histories, never()).save(any());
        verify(fixture.operationLogs, never()).save(any());
    }

    @Test
    void operationKeyCannotBeReusedForDifferentCommand() {
        Fixture fixture = fixture(order(OrderStatus.PACKING, PaymentTiming.POSTPAID, PaymentMethod.COD));
        OrderCommandResponse stored = new OrderCommandResponse(
                ORDER_ID, OrderStatus.PACKING, List.of(OrderStatus.PACKING), NOW, false);
        when(fixture.operationLogs.findById(OPERATION_KEY)).thenReturn(Optional.of(OrderOperationLog.completed(
                OPERATION_KEY, "ADMIN_PACK", ORDER_ID,
                fixture.objectMapper.writeValueAsString(stored), NOW)));

        OrderException error = assertThrows(
                OrderException.class,
                () -> fixture.service.adminShip(ADMIN_ID, ORDER_ID, OPERATION_KEY));

        assertEquals("IDEMPOTENCY_KEY_REUSED", error.getCode());
    }

    private Fixture fixture(CustomerOrder order) {
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderOperationLogRepository operationLogs = Mockito.mock(OrderOperationLogRepository.class);
        OrderStatusHistoryRepository histories = Mockito.mock(OrderStatusHistoryRepository.class);
        OutboxEventRepository outbox = Mockito.mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(orders.findForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(orders.findOwnedForUpdate(ORDER_ID, CUSTOMER_ID)).thenReturn(Optional.of(order));
        when(operationLogs.findById(OPERATION_KEY)).thenReturn(Optional.empty());
        OrderLifecycleCommandService service = new OrderLifecycleCommandService(
                orders, operationLogs, histories, outbox, new OrderStateMachine(), objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, order, orders, operationLogs, histories, outbox, objectMapper);
    }

    private CustomerOrder order(OrderStatus status, PaymentTiming timing, PaymentMethod method) {
        return CustomerOrder.create(
                ORDER_ID, "ORD-COMMAND", UUID.randomUUID(), CUSTOMER_ID, status, timing, method,
                100_000, 0, 100_000, 0, 0, 20_000, 0, 120_000, null, NOW.minusSeconds(60));
    }

    private record Fixture(
            OrderLifecycleCommandService service,
            CustomerOrder order,
            CustomerOrderRepository orders,
            OrderOperationLogRepository operationLogs,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox,
            ObjectMapper objectMapper) {
    }
}
