package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderPaymentCheckpointServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");
    private static final Instant DUE_AT = Instant.parse("2026-09-24T08:00:00Z");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void persistsPaymentIdDueAtAndSagaCheckpointTogether() {
        Fixture fixture = paidFixture();

        fixture.service.markPaymentRequested(SAGA_ID, ORDER_ID, PAYMENT_ID, DUE_AT);

        assertEquals(SagaStatus.PAYMENT_REQUESTED, fixture.saga.getStatus());
        assertEquals(PAYMENT_ID, fixture.saga.getPaymentId());
        assertEquals("PAYMENT_CONTEXT_CREATED", fixture.saga.getCurrentStep());
        assertEquals(DUE_AT, fixture.order.getPaymentDueAt());
        assertEquals(NOW, fixture.order.getUpdatedAt());
    }

    @Test
    void samePaymentCheckpointCanReplayButDifferentPaymentIdIsRejected() {
        Fixture fixture = paidFixture();
        fixture.service.markPaymentRequested(SAGA_ID, ORDER_ID, PAYMENT_ID, DUE_AT);

        fixture.service.markPaymentRequested(SAGA_ID, ORDER_ID, PAYMENT_ID, DUE_AT);
        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.markPaymentRequested(SAGA_ID, ORDER_ID, UUID.randomUUID(), DUE_AT));

        assertEquals("PAYMENT_CHECKPOINT_MISMATCH", exception.getCode());
    }

    @Test
    void zeroValueOrderRecordsPaymentNotRequiredWithoutInventingPaymentId() {
        Fixture fixture = freeFixture();

        fixture.service.markPaymentNotRequired(SAGA_ID, ORDER_ID);

        assertEquals(SagaStatus.ORDER_CREATED, fixture.saga.getStatus());
        assertEquals("PAYMENT_NOT_REQUIRED", fixture.saga.getCurrentStep());
        assertNull(fixture.saga.getPaymentId());
    }

    @Test
    void paidOrderCannotUseNoPaymentCheckpoint() {
        Fixture fixture = paidFixture();

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.markPaymentNotRequired(SAGA_ID, ORDER_ID));

        assertEquals("INVALID_PAYMENT_CHECKPOINT", exception.getCode());
    }

    private Fixture paidFixture() {
        return fixture(125_000, PaymentTiming.PREPAID, PaymentMethod.VNPAY);
    }

    private Fixture freeFixture() {
        return fixture(0, PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE);
    }

    private Fixture fixture(long total, PaymentTiming timing, PaymentMethod method) {
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), NOW.minusSeconds(60));
        saga.markOrderCreated(ORDER_ID, NOW.minusSeconds(30));
        CustomerOrder order = CustomerOrder.create(
                ORDER_ID, "ORD-1", saga.getCheckoutSessionId(), UUID.randomUUID(),
                total > 0 ? OrderStatus.PENDING_PAYMENT : OrderStatus.CONFIRMED,
                timing, method, total, 0, total, 0, 0, 0, 0, total, null, NOW.minusSeconds(30));
        when(sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));
        when(orders.findForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        return new Fixture(
                saga, order, new OrderPaymentCheckpointService(
                        sagas, orders, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private record Fixture(OrderSaga saga, CustomerOrder order, OrderPaymentCheckpointService service) {
    }
}
