package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.config.SagaRecoveryProperties;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.service.OrderSagaRecoveryLeaseService.RecoveryClaim;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderSagaRecoveryWorkerTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void resumesNormalSagaThroughOwnedIdempotentOrchestrator() {
        Fixture fixture = fixture(claim(SagaStatus.ORDER_CREATED));

        var result = fixture.worker.recoverBatch();

        assertEquals(1, result.recovered());
        assertEquals(0, result.failed());
        verify(fixture.orderCreation).create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);
        verify(fixture.compensation, never()).resume(SAGA_ID);
        verify(fixture.leases).release(Mockito.eq(SAGA_ID), anyString());
    }

    @Test
    void resumesCompensatingSagaWithoutReenteringCreateOrder() {
        Fixture fixture = fixture(claim(SagaStatus.COMPENSATING));

        fixture.worker.recoverBatch();

        verify(fixture.compensation).resume(SAGA_ID);
        verify(fixture.orderCreation, never()).create(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void failureIsRecordedAndDoesNotEscapeScheduledBatch() {
        Fixture fixture = fixture(claim(SagaStatus.ORDER_CREATED));
        IllegalStateException failure = new IllegalStateException("payment unavailable");
        when(fixture.orderCreation.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY)).thenThrow(failure);

        var result = fixture.worker.recoverBatch();

        assertEquals(0, result.recovered());
        assertEquals(1, result.failed());
        verify(fixture.leases).recordFailure(Mockito.eq(SAGA_ID), anyString(), Mockito.eq(0), Mockito.same(failure));
        verify(fixture.leases, never()).release(Mockito.eq(SAGA_ID), anyString());
    }

    private Fixture fixture(RecoveryClaim claim) {
        OrderSagaRecoveryLeaseService leases = Mockito.mock(OrderSagaRecoveryLeaseService.class);
        OrderCreationOrchestrator orderCreation = Mockito.mock(OrderCreationOrchestrator.class);
        OrderCompensationRecoveryService compensation = Mockito.mock(OrderCompensationRecoveryService.class);
        when(leases.claimNext(anyString()))
                .thenReturn(Optional.of(claim))
                .thenReturn(Optional.empty());
        SagaRecoveryProperties properties = new SagaRecoveryProperties(
                true, Duration.ofSeconds(30), Duration.ZERO, Duration.ofSeconds(30),
                Duration.ofMinutes(2), Duration.ofMinutes(1), 20);
        return new Fixture(leases, orderCreation, compensation,
                new OrderSagaRecoveryWorker(leases, orderCreation, compensation, properties));
    }

    private RecoveryClaim claim(SagaStatus status) {
        return new RecoveryClaim(
                SAGA_ID, status, CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY, UUID.randomUUID(), 0);
    }

    private record Fixture(
            OrderSagaRecoveryLeaseService leases,
            OrderCreationOrchestrator orderCreation,
            OrderCompensationRecoveryService compensation,
            OrderSagaRecoveryWorker worker) {
    }
}
