package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderReservationFinalizationCheckpointServiceTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-09-30T04:00:00Z");

    @Test
    void startsAfterPaidPaymentCheckpointAndCompletesAllReservations() {
        Fixture fixture = fixture(paidSaga());

        fixture.service.begin(SAGA_ID, ORDER_ID);
        assertEquals(SagaStatus.FINALIZING_RESERVATIONS, fixture.saga.getStatus());
        assertEquals("COMMITTING_INVENTORY", fixture.saga.getCurrentStep());

        fixture.service.markInventoryCommitted(SAGA_ID, ORDER_ID);
        assertEquals(SagaStatus.INVENTORY_COMMITTED, fixture.saga.getStatus());

        fixture.service.markCompleted(SAGA_ID, ORDER_ID);
        assertEquals(SagaStatus.COMPLETED, fixture.saga.getStatus());
        assertEquals("RESERVATIONS_FINALIZED", fixture.saga.getCurrentStep());
        assertEquals(NOW, fixture.saga.getCompletedAt());
    }

    @Test
    void freeOrderCanStartWithoutPaymentId() {
        OrderSaga saga = orderCreatedSaga();
        saga.markPaymentNotRequired(NOW.minusSeconds(10));
        Fixture fixture = fixture(saga);

        fixture.service.begin(SAGA_ID, ORDER_ID);

        assertEquals(SagaStatus.FINALIZING_RESERVATIONS, saga.getStatus());
        assertNull(saga.getPaymentId());
    }

    @Test
    void orderCreatedWithoutNoPaymentCheckpointCannotStart() {
        Fixture fixture = fixture(orderCreatedSaga());

        OrderException failure = assertThrows(
                OrderException.class, () -> fixture.service.begin(SAGA_ID, ORDER_ID));

        assertEquals("RESERVATION_FINALIZATION_NOT_READY", failure.getCode());
    }

    @Test
    void recordsRetryAtTheCorrectRemoteBoundary() {
        Fixture inventoryFixture = fixture(paidSaga());
        inventoryFixture.service.begin(SAGA_ID, ORDER_ID);
        inventoryFixture.service.recordFailure(SAGA_ID, ORDER_ID, new IllegalStateException("inventory down"));
        assertEquals("INVENTORY_COMMIT_RETRY_REQUIRED", inventoryFixture.saga.getCurrentStep());
        assertEquals(1, inventoryFixture.saga.getAttemptCount());

        Fixture voucherFixture = fixture(paidSaga());
        voucherFixture.service.begin(SAGA_ID, ORDER_ID);
        voucherFixture.service.markInventoryCommitted(SAGA_ID, ORDER_ID);
        voucherFixture.service.recordFailure(SAGA_ID, ORDER_ID, new IllegalStateException("voucher down"));
        assertEquals("VOUCHER_CONSUME_RETRY_REQUIRED", voucherFixture.saga.getCurrentStep());
        assertEquals(1, voucherFixture.saga.getAttemptCount());
    }

    @Test
    void rejectsAnOrderDifferentFromSagaCheckpoint() {
        Fixture fixture = fixture(paidSaga());

        OrderException failure = assertThrows(
                OrderException.class, () -> fixture.service.begin(SAGA_ID, UUID.randomUUID()));

        assertEquals("ORDER_SAGA_ORDER_MISMATCH", failure.getCode());
    }

    private Fixture fixture(OrderSaga saga) {
        OrderSagaRepository repository = Mockito.mock(OrderSagaRepository.class);
        when(repository.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new Fixture(saga, new OrderReservationFinalizationCheckpointService(repository, clock));
    }

    private OrderSaga paidSaga() {
        OrderSaga saga = orderCreatedSaga();
        saga.markPaymentRequested(PAYMENT_ID, NOW.minusSeconds(10));
        return saga;
    }

    private OrderSaga orderCreatedSaga() {
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(),
                NOW.minusSeconds(30));
        saga.markOrderCreated(ORDER_ID, NOW.minusSeconds(20));
        return saga;
    }

    private record Fixture(
            OrderSaga saga,
            OrderReservationFinalizationCheckpointService service) {
    }
}
