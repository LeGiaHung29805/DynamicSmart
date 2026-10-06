package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ProcessedEvent;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.messaging.PaymentEventEnvelope;
import com.dynamicmart.order_service.messaging.PaymentEventEnvelope.PaymentPayload;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OrderStatusHistoryRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.repository.ProcessedEventRepository;
import com.dynamicmart.order_service.service.OrderPaymentEventService.FollowUpAction;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

class OrderPaymentEventServiceTests {
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-02T01:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-02T01:01:00Z");

    @Test
    void prepaidSuccessConfirmsOrderAndRequestsReservationFinalization() {
        Fixture fixture = fixture(OrderStatus.PENDING_PAYMENT, SagaStatus.PAYMENT_REQUESTED);

        var result = fixture.service.apply(event("PaymentSucceeded", "PAID", 120_000));

        assertEquals(OrderStatus.CONFIRMED, fixture.order.getStatus());
        assertEquals(OCCURRED_AT, fixture.order.getPaymentSucceededAt());
        assertEquals(OCCURRED_AT, fixture.order.getConfirmedAt());
        assertEquals(FollowUpAction.FINALIZE_RESERVATIONS, result.followUpAction());
        assertTrue(!result.replay());
        verify(fixture.histories).save(any());
        verify(fixture.processedEvents).save(any());
        verify(fixture.outbox, never()).save(any());
    }

    @Test
    void prepaidFailureCancelsOrderAndBeginsDurableCompensation() {
        Fixture fixture = fixture(OrderStatus.PENDING_PAYMENT, SagaStatus.PAYMENT_REQUESTED);

        var result = fixture.service.apply(event("PaymentFailed", "FAILED", 120_000));

        assertEquals(OrderStatus.CANCELLED, fixture.order.getStatus());
        assertEquals(OCCURRED_AT, fixture.order.getCancelledAt());
        assertEquals("PAYMENT_FAILED", fixture.order.getCancelReason());
        assertEquals(SagaStatus.COMPENSATING, fixture.saga.getStatus());
        assertEquals(FollowUpAction.COMPENSATE_RESERVATIONS, result.followUpAction());
        verify(fixture.histories).save(any());
        verify(fixture.outbox).save(any());
        verify(fixture.processedEvents).save(any());
    }

    @Test
    void duplicateSuccessDoesNotWriteAgainButRetriesUnfinishedFollowUp() {
        Fixture fixture = fixture(OrderStatus.CONFIRMED, SagaStatus.PAYMENT_REQUESTED);
        fixture.order.setPaymentSucceededAt(OCCURRED_AT);
        ProcessedEvent processed = ProcessedEvent.record(
                EVENT_ID, "PaymentSucceeded", "payment-service", NOW, CORRELATION_ID);
        when(fixture.processedEvents.findById(EVENT_ID)).thenReturn(Optional.of(processed));

        var result = fixture.service.apply(event("PaymentSucceeded", "PAID", 120_000));

        assertTrue(result.replay());
        assertEquals(FollowUpAction.FINALIZE_RESERVATIONS, result.followUpAction());
        verify(fixture.histories, never()).save(any());
        verify(fixture.processedEvents, never()).save(any());
    }

    @Test
    void postpaidSuccessAfterDeliveryCompletesOrderAndEmitsOutboxEvent() {
        Fixture fixture = fixture(
                OrderStatus.DELIVERED,
                SagaStatus.COMPLETED,
                PaymentTiming.POSTPAID,
                PaymentMethod.COD);

        var result = fixture.service.apply(event(
                "PaymentSucceeded", "PAID", 120_000, PaymentTiming.POSTPAID, PaymentMethod.COD));

        assertEquals(OrderStatus.COMPLETED, fixture.order.getStatus());
        assertEquals(OCCURRED_AT, fixture.order.getCompletedAt());
        assertEquals(FollowUpAction.NONE, result.followUpAction());
        ArgumentCaptor<com.dynamicmart.order_service.entity.OutboxEvent> event =
                ArgumentCaptor.forClass(com.dynamicmart.order_service.entity.OutboxEvent.class);
        verify(fixture.outbox).save(event.capture());
        assertEquals("OrderCompleted", event.getValue().getEventType());
    }

    @Test
    void mismatchedAmountIsRejectedBeforeAnyStateChange() {
        Fixture fixture = fixture(OrderStatus.PENDING_PAYMENT, SagaStatus.PAYMENT_REQUESTED);

        OrderException error = assertThrows(
                OrderException.class,
                () -> fixture.service.apply(event("PaymentSucceeded", "PAID", 119_999)));

        assertEquals("PAYMENT_EVENT_CONTRACT_MISMATCH", error.getCode());
        assertEquals(OrderStatus.PENDING_PAYMENT, fixture.order.getStatus());
        verify(fixture.processedEvents, never()).save(any());
    }

    @Test
    void reusedEventIdWithDifferentMetadataIsRejected() {
        Fixture fixture = fixture(OrderStatus.CONFIRMED, SagaStatus.PAYMENT_REQUESTED);
        ProcessedEvent processed = ProcessedEvent.record(
                EVENT_ID, "PaymentExpired", "payment-service", NOW, CORRELATION_ID);
        when(fixture.processedEvents.findById(EVENT_ID)).thenReturn(Optional.of(processed));

        OrderException error = assertThrows(
                OrderException.class,
                () -> fixture.service.apply(event("PaymentSucceeded", "PAID", 120_000)));

        assertEquals("PAYMENT_EVENT_ID_REUSED", error.getCode());
    }

    private Fixture fixture(OrderStatus status, SagaStatus sagaStatus) {
        return fixture(status, sagaStatus, PaymentTiming.PREPAID, PaymentMethod.VNPAY);
    }

    private Fixture fixture(
            OrderStatus status,
            SagaStatus sagaStatus,
            PaymentTiming timing,
            PaymentMethod method) {
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        OrderStatusHistoryRepository histories = Mockito.mock(OrderStatusHistoryRepository.class);
        OutboxEventRepository outbox = Mockito.mock(OutboxEventRepository.class);
        ProcessedEventRepository processedEvents = Mockito.mock(ProcessedEventRepository.class);
        CustomerOrder order = CustomerOrder.create(
                ORDER_ID, "ORD-TEST", UUID.randomUUID(), CUSTOMER_ID, status, timing, method,
                120_000, 0, 120_000, 0, 0, 0, 0, 120_000, null, NOW.minusSeconds(60));
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, order.getCheckoutSessionId(), UUID.randomUUID(), "a".repeat(64),
                CORRELATION_ID, NOW.minusSeconds(60));
        saga.setOrderId(ORDER_ID);
        saga.setPaymentId(PAYMENT_ID);
        saga.setStatus(sagaStatus);
        saga.setInventoryReservationId(UUID.randomUUID());
        when(orders.findForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(sagas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(saga));
        when(sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));
        when(processedEvents.findById(EVENT_ID)).thenReturn(Optional.empty());
        ObjectMapper objectMapper = new ObjectMapper();
        OrderPaymentEventService service = new OrderPaymentEventService(
                orders, sagas, histories, outbox, processedEvents, new OrderStateMachine(), objectMapper,
                Clock.fixed(NOW, ZoneOffset.UTC));
        assertNotNull(service);
        return new Fixture(service, order, saga, histories, outbox, processedEvents);
    }

    private PaymentEventEnvelope event(String type, String status, long amount) {
        return event(type, status, amount, PaymentTiming.PREPAID, PaymentMethod.VNPAY);
    }

    private PaymentEventEnvelope event(
            String type,
            String status,
            long amount,
            PaymentTiming timing,
            PaymentMethod method) {
        return new PaymentEventEnvelope(
                EVENT_ID, type, 1, "payment-service", PAYMENT_ID, OCCURRED_AT, CORRELATION_ID,
                new PaymentPayload(PAYMENT_ID, ORDER_ID, amount, timing.name(), method.name(), status));
    }

    private record Fixture(
            OrderPaymentEventService service,
            CustomerOrder order,
            OrderSaga saga,
            OrderStatusHistoryRepository histories,
            OutboxEventRepository outbox,
            ProcessedEventRepository processedEvents) {
    }
}
