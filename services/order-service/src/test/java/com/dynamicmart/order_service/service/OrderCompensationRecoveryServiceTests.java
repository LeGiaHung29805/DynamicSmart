package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class OrderCompensationRecoveryServiceTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INVENTORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void resumesReverseOrderCompensationWithStableOperationKeys() {
        Fixture fixture = fixture();
        InOrder order = inOrder(fixture.inventory, fixture.vouchers, fixture.checkpoints);

        fixture.service.resume(SAGA_ID);

        order.verify(fixture.inventory).release(any());
        order.verify(fixture.vouchers).release(any());
        order.verify(fixture.checkpoints).markCompensated(SAGA_ID);
        var inventoryRequest = Mockito.mockingDetails(fixture.inventory).getInvocations().stream()
                .findFirst().orElseThrow().getArgument(0, InventoryReservationGateway.ReleaseInventoryRequest.class);
        assertEquals(OrderReservationService.operationKey(SAGA_ID, "INVENTORY_RELEASE"),
                inventoryRequest.operationKey());
    }

    @Test
    void releaseFailureIsCheckpointedAndPropagatedForBackoff() {
        Fixture fixture = fixture();
        IllegalStateException failure = new IllegalStateException("catalog unavailable");
        Mockito.doThrow(failure).when(fixture.inventory).release(any());

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, () -> fixture.service.resume(SAGA_ID));

        assertEquals(failure, thrown);
        verify(fixture.checkpoints).recordCompensationFailure(SAGA_ID, failure);
    }

    private Fixture fixture() {
        InventoryReservationGateway inventory = Mockito.mock(InventoryReservationGateway.class);
        VoucherReservationGateway vouchers = Mockito.mock(VoucherReservationGateway.class);
        OrderSagaCheckpointService checkpoints = Mockito.mock(OrderSagaCheckpointService.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(),
                Instant.parse("2026-09-30T09:00:00Z"));
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setInventoryReservationId(INVENTORY_ID);
        saga.setVoucherReservationId(VOUCHER_ID);
        saga.setLastErrorCode("OUT_OF_STOCK");
        when(sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga));
        return new Fixture(inventory, vouchers, checkpoints,
                new OrderCompensationRecoveryService(inventory, vouchers, checkpoints, sagas));
    }

    private record Fixture(
            InventoryReservationGateway inventory,
            VoucherReservationGateway vouchers,
            OrderSagaCheckpointService checkpoints,
            OrderCompensationRecoveryService service) {
    }
}
