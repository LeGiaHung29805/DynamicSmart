package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class OrderReservationFinalizationServiceTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID INVENTORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID VOUCHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Test
    void prepaidOrderWaitsWithoutFinalizingReservations() {
        Fixture fixture = fixture(saga(SagaStatus.PAYMENT_REQUESTED, true), order(OrderStatus.PENDING_PAYMENT, 100));

        var result = fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID);

        assertTrue(result.waitingForPayment());
        verify(fixture.checkpoints, never()).begin(any(), any());
        verify(fixture.inventory, never()).commit(any());
        verify(fixture.vouchers, never()).consume(any());
    }

    @Test
    void confirmedOrderCommitsInventoryThenConsumesVoucherWithStableKeys() {
        Fixture fixture = fixture(saga(SagaStatus.PAYMENT_REQUESTED, true), order(OrderStatus.CONFIRMED, 100));
        InOrder sequence = inOrder(fixture.checkpoints, fixture.inventory, fixture.vouchers);

        var result = fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID);

        assertTrue(result.completed());
        sequence.verify(fixture.checkpoints).begin(SAGA_ID, ORDER_ID);
        sequence.verify(fixture.inventory).commit(any());
        sequence.verify(fixture.checkpoints).markInventoryCommitted(SAGA_ID, ORDER_ID);
        sequence.verify(fixture.vouchers).consume(any());
        sequence.verify(fixture.checkpoints).markCompleted(SAGA_ID, ORDER_ID);

        ArgumentCaptor<InventoryReservationGateway.CommitInventoryRequest> inventoryRequest =
                ArgumentCaptor.forClass(InventoryReservationGateway.CommitInventoryRequest.class);
        verify(fixture.inventory).commit(inventoryRequest.capture());
        assertEquals(OrderReservationService.operationKey(SAGA_ID, "INVENTORY_COMMIT"),
                inventoryRequest.getValue().operationKey());
        assertEquals(ORDER_ID, inventoryRequest.getValue().orderId());

        ArgumentCaptor<VoucherReservationGateway.ConsumeVoucherRequest> voucherRequest =
                ArgumentCaptor.forClass(VoucherReservationGateway.ConsumeVoucherRequest.class);
        verify(fixture.vouchers).consume(voucherRequest.capture());
        assertEquals(OrderReservationService.operationKey(SAGA_ID, "VOUCHER_CONSUME"),
                voucherRequest.getValue().operationKey());
    }

    @Test
    void freeOrderWithoutVoucherSkipsVoucherRemoteCall() {
        OrderSaga saga = saga(SagaStatus.ORDER_CREATED, false);
        saga.markPaymentNotRequired(Instant.now());
        saga.setVoucherReservationId(null);
        Fixture fixture = fixture(saga, order(OrderStatus.CONFIRMED, 0));

        var result = fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID);

        assertTrue(result.completed());
        verify(fixture.inventory).commit(any());
        verify(fixture.vouchers, never()).consume(any());
        verify(fixture.checkpoints).markCompleted(SAGA_ID, ORDER_ID);
    }

    @Test
    void retryAfterInventoryCheckpointSkipsInventoryAndResumesVoucher() {
        Fixture fixture = fixture(saga(SagaStatus.INVENTORY_COMMITTED, true), order(OrderStatus.CONFIRMED, 100));

        fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID);

        verify(fixture.checkpoints, never()).begin(any(), any());
        verify(fixture.inventory, never()).commit(any());
        verify(fixture.vouchers).consume(any());
        verify(fixture.checkpoints).markCompleted(SAGA_ID, ORDER_ID);
    }

    @Test
    void remoteFailureIsCheckpointedAndRethrown() {
        Fixture fixture = fixture(saga(SagaStatus.PAYMENT_REQUESTED, true), order(OrderStatus.CONFIRMED, 100));
        OrderException failure = new OrderException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_DOWN", "down");
        Mockito.doThrow(failure).when(fixture.inventory).commit(any());

        OrderException thrown = assertThrows(
                OrderException.class, () -> fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID));

        assertEquals(failure, thrown);
        verify(fixture.checkpoints).recordFailure(SAGA_ID, ORDER_ID, failure);
        verify(fixture.vouchers, never()).consume(any());
    }

    @Test
    void checkpointFailureDoesNotHideTheRemoteFailure() {
        Fixture fixture = fixture(saga(SagaStatus.PAYMENT_REQUESTED, true), order(OrderStatus.CONFIRMED, 100));
        IllegalStateException remoteFailure = new IllegalStateException("inventory down");
        IllegalStateException checkpointFailure = new IllegalStateException("database down");
        Mockito.doThrow(remoteFailure).when(fixture.inventory).commit(any());
        Mockito.doThrow(checkpointFailure).when(fixture.checkpoints)
                .recordFailure(SAGA_ID, ORDER_ID, remoteFailure);

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class, () -> fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID));

        assertEquals(remoteFailure, thrown);
        assertEquals(checkpointFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void missingInventoryCheckpointIsRejectedBeforeRemoteCalls() {
        OrderSaga saga = saga(SagaStatus.PAYMENT_REQUESTED, true);
        saga.setInventoryReservationId(null);
        Fixture fixture = fixture(saga, order(OrderStatus.CONFIRMED, 100));

        OrderException failure = assertThrows(
                OrderException.class, () -> fixture.service.finalizeIfConfirmed(SAGA_ID, ORDER_ID));

        assertEquals("INVENTORY_RESERVATION_CHECKPOINT_MISSING", failure.getCode());
        verify(fixture.inventory, never()).commit(any());
    }

    private Fixture fixture(OrderSaga saga, CustomerOrder order) {
        InventoryReservationGateway inventory = Mockito.mock(InventoryReservationGateway.class);
        VoucherReservationGateway vouchers = Mockito.mock(VoucherReservationGateway.class);
        OrderReservationFinalizationCheckpointService checkpoints =
                Mockito.mock(OrderReservationFinalizationCheckpointService.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        when(sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga));
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
        return new Fixture(inventory, vouchers, checkpoints,
                new OrderReservationFinalizationService(inventory, vouchers, checkpoints, sagas, orders));
    }

    private OrderSaga saga(SagaStatus status, boolean withVoucher) {
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), CORRELATION_ID,
                Instant.now().minusSeconds(30));
        saga.markOrderCreated(ORDER_ID, Instant.now().minusSeconds(20));
        saga.setInventoryReservationId(INVENTORY_ID);
        saga.setVoucherReservationId(withVoucher ? VOUCHER_ID : null);
        if (status != SagaStatus.ORDER_CREATED) {
            saga.markPaymentRequested(UUID.randomUUID(), Instant.now().minusSeconds(10));
            saga.setStatus(status);
        }
        return saga;
    }

    private CustomerOrder order(OrderStatus status, long total) {
        return CustomerOrder.create(
                ORDER_ID, "ORD-1", UUID.randomUUID(), UUID.randomUUID(), status,
                total == 0 ? PaymentTiming.POSTPAID : PaymentTiming.PREPAID,
                total == 0 ? PaymentMethod.FREE : PaymentMethod.VNPAY,
                total, 0, total, 0, 0, 0, 0, total, null, Instant.now());
    }

    private record Fixture(
            InventoryReservationGateway inventory,
            VoucherReservationGateway vouchers,
            OrderReservationFinalizationCheckpointService checkpoints,
            OrderReservationFinalizationService service) {
    }
}
