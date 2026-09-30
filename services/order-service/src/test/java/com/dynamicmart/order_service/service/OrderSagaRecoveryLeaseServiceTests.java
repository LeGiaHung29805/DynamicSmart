package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.config.SagaRecoveryProperties;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderSagaRecoveryLeaseServiceTests {
    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void claimsStaleSagaWithDurableLeaseAndCustomerContext() {
        Fixture fixture = fixture();
        OrderSaga saga = saga(SagaStatus.ORDER_CREATED);
        CheckoutSession session = CheckoutSession.create(
                SESSION_ID, CUSTOMER_ID, CheckoutSource.CART, UUID.randomUUID(), "a".repeat(64),
                NOW.plusSeconds(60), NOW.minusSeconds(120));
        when(fixture.sagas.findNextRecoveryCandidateForUpdate(NOW.minusSeconds(30), NOW))
                .thenReturn(Optional.of(saga));
        when(fixture.sessions.findById(SESSION_ID)).thenReturn(Optional.of(session));

        var claim = fixture.service.claimNext("worker-1").orElseThrow();

        assertEquals(SAGA_ID, claim.sagaId());
        assertEquals(CUSTOMER_ID, claim.customerId());
        assertEquals("worker-1", saga.getRecoveryOwner());
        assertEquals(NOW.plusSeconds(120), saga.getRecoveryLeaseUntil());
    }

    @Test
    void releaseDoesNotClearLeaseOwnedByAnotherWorker() {
        Fixture fixture = fixture();
        OrderSaga saga = saga(SagaStatus.STARTED);
        saga.setRecoveryOwner("worker-2");
        saga.setRecoveryLeaseUntil(NOW.plusSeconds(120));
        when(fixture.sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));

        fixture.service.release(SAGA_ID, "worker-1");

        assertEquals("worker-2", saga.getRecoveryOwner());
        assertEquals(NOW.plusSeconds(120), saga.getRecoveryLeaseUntil());
    }

    @Test
    void failurePersistsBackoffAndDiagnosticWithoutChangingSagaStatus() {
        Fixture fixture = fixture();
        OrderSaga saga = saga(SagaStatus.ORDER_CREATED);
        saga.setRecoveryOwner("worker-1");
        saga.setRecoveryLeaseUntil(NOW.plusSeconds(120));
        when(fixture.sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));

        fixture.service.recordFailure(SAGA_ID, "worker-1", 0, new IllegalStateException("payment unavailable"));

        assertEquals(SagaStatus.ORDER_CREATED, saga.getStatus());
        assertEquals(1, saga.getAttemptCount());
        assertEquals("IllegalStateException", saga.getLastErrorCode());
        assertEquals("payment unavailable", saga.getLastErrorMessage());
        assertEquals(NOW.plusSeconds(60), saga.getRecoveryLeaseUntil());
        assertTrue(saga.getRecoveryOwner() == null);
        verify(fixture.sagas).findByIdForUpdate(SAGA_ID);
    }

    @Test
    void failureDoesNotDoubleCountAttemptAlreadyRecordedByCheckpointService() {
        Fixture fixture = fixture();
        OrderSaga saga = saga(SagaStatus.FINALIZING_RESERVATIONS);
        saga.setRecoveryOwner("worker-1");
        saga.setAttemptCount(2);
        when(fixture.sagas.findByIdForUpdate(SAGA_ID)).thenReturn(Optional.of(saga));

        fixture.service.recordFailure(SAGA_ID, "worker-1", 1, new IllegalStateException("voucher unavailable"));

        assertEquals(2, saga.getAttemptCount());
        assertEquals(NOW.plusSeconds(60), saga.getRecoveryLeaseUntil());
    }

    private Fixture fixture() {
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CheckoutSessionRepository sessions = Mockito.mock(CheckoutSessionRepository.class);
        SagaRecoveryProperties properties = new SagaRecoveryProperties(
                true, Duration.ofSeconds(30), Duration.ZERO, Duration.ofSeconds(30),
                Duration.ofMinutes(2), Duration.ofMinutes(1), 20);
        return new Fixture(sagas, sessions, new OrderSagaRecoveryLeaseService(
                sagas, sessions, properties, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private OrderSaga saga(SagaStatus status) {
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, SESSION_ID, UUID.randomUUID(), "a".repeat(64), UUID.randomUUID(),
                NOW.minusSeconds(120));
        saga.setStatus(status);
        return saga;
    }

    private record Fixture(
            OrderSagaRepository sagas,
            CheckoutSessionRepository sessions,
            OrderSagaRecoveryLeaseService service) {
    }
}
