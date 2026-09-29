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

class OrderSagaCheckpointServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-24T04:00:00Z");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID VOUCHER_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INVENTORY_RESERVATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void persistsVoucherThenInventoryCheckpoints() {
        Fixture fixture = fixture(startedSaga());

        fixture.service.markVoucherReserved(SAGA_ID, VOUCHER_RESERVATION_ID);
        assertEquals(SagaStatus.VOUCHER_RESERVED, fixture.saga.getStatus());
        assertEquals(VOUCHER_RESERVATION_ID, fixture.saga.getVoucherReservationId());
        assertEquals("VOUCHER_RESERVED", fixture.saga.getCurrentStep());

        fixture.service.markInventoryReserved(SAGA_ID, INVENTORY_RESERVATION_ID);
        assertEquals(SagaStatus.INVENTORY_RESERVED, fixture.saga.getStatus());
        assertEquals(INVENTORY_RESERVATION_ID, fixture.saga.getInventoryReservationId());
        assertEquals("INVENTORY_RESERVED", fixture.saga.getCurrentStep());
    }

    @Test
    void allowsVoucherCheckpointWithoutReservationWhenNoVoucherWasSelected() {
        Fixture fixture = fixture(startedSaga());

        fixture.service.markVoucherReserved(SAGA_ID, null);

        assertEquals(SagaStatus.VOUCHER_RESERVED, fixture.saga.getStatus());
        assertNull(fixture.saga.getVoucherReservationId());
    }

    @Test
    void sameCheckpointCanBeReplayedButDifferentReservationIsRejected() {
        OrderSaga saga = startedSaga();
        saga.setStatus(SagaStatus.VOUCHER_RESERVED);
        saga.setVoucherReservationId(VOUCHER_RESERVATION_ID);
        Fixture fixture = fixture(saga);

        fixture.service.markVoucherReserved(SAGA_ID, VOUCHER_RESERVATION_ID);
        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.markVoucherReserved(SAGA_ID, UUID.randomUUID()));

        assertEquals("VOUCHER_RESERVATION_MISMATCH", exception.getCode());
    }

    @Test
    void inventoryCheckpointRequiresVoucherCheckpointFirst() {
        Fixture fixture = fixture(startedSaga());

        OrderException exception = assertThrows(OrderException.class,
                () -> fixture.service.markInventoryReserved(SAGA_ID, INVENTORY_RESERVATION_ID));

        assertEquals("INVALID_SAGA_CHECKPOINT", exception.getCode());
    }

    @Test
    void recordsCompensationLifecycleAsDurableTerminalState() {
        OrderSaga saga = startedSaga();
        saga.setStatus(SagaStatus.VOUCHER_RESERVED);
        saga.setVoucherReservationId(VOUCHER_RESERVATION_ID);
        Fixture fixture = fixture(saga);

        fixture.service.beginCompensation(SAGA_ID, "INVENTORY_UNAVAILABLE", "Inventory tạm lỗi");
        assertEquals(SagaStatus.COMPENSATING, saga.getStatus());
        assertEquals("RELEASING_RESERVATIONS", saga.getCurrentStep());
        assertEquals(1, saga.getAttemptCount());

        fixture.service.markCompensated(SAGA_ID);
        assertEquals(SagaStatus.COMPENSATED, saga.getStatus());
        assertEquals("RESERVATIONS_RELEASED", saga.getCurrentStep());
        assertEquals(NOW, saga.getCompletedAt());
    }

    @Test
    void leavesRetryableCheckpointWhenCompensationFails() {
        OrderSaga saga = startedSaga();
        saga.setStatus(SagaStatus.COMPENSATING);
        Fixture fixture = fixture(saga);

        fixture.service.recordCompensationFailure(SAGA_ID, new IllegalStateException("release failed"));

        assertEquals(SagaStatus.COMPENSATING, saga.getStatus());
        assertEquals("COMPENSATION_RETRY_REQUIRED", saga.getCurrentStep());
        assertEquals("IllegalStateException", saga.getLastErrorCode());
        assertEquals("release failed", saga.getLastErrorMessage());
    }

    private Fixture fixture(OrderSaga saga) {
        OrderSagaRepository repository = Mockito.mock(OrderSagaRepository.class);
        when(repository.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));
        return new Fixture(saga, new OrderSagaCheckpointService(
                repository, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private OrderSaga startedSaga() {
        return OrderSaga.start(
                SAGA_ID, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(), NOW.minusSeconds(30));
    }

    private record Fixture(OrderSaga saga, OrderSagaCheckpointService service) {
    }
}
