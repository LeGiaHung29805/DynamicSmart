package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.OrderCreationAdmissionService.AdmissionResult;
import com.dynamicmart.order_service.service.CheckoutPricingService.PricingBreakdown;
import com.dynamicmart.order_service.service.OrderCreationContextReader.CreationContext;
import com.dynamicmart.order_service.service.OrderCreationPersistenceService.PersistedOrder;
import com.dynamicmart.order_service.service.OrderCreationRevalidationService.ValidatedOrderInput;
import com.dynamicmart.order_service.service.OrderReservationService.ReservationResult;
import com.dynamicmart.order_service.service.OrderPaymentCreationService.PaymentCheckpoint;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class OrderCreationOrchestratorTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void coordinatesEveryBoundaryInOrderAndReturnsCreatedOrder() {
        Fixture fixture = fixture(false);

        var result = fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);

        assertEquals(fixture.persisted.orderId(), result.orderId());
        verify(fixture.revalidation).revalidate(CUSTOMER_ID, SAGA_ID);
        verify(fixture.reservations).reserve(fixture.input);
        verify(fixture.quoteConsumption).consume(fixture.input);
        verify(fixture.persistence).persist(fixture.input, fixture.reserved);
        verify(fixture.paymentCreation).ensurePayment(any());
        verify(fixture.reservationFinalization)
                .finalizeIfConfirmed(SAGA_ID, fixture.persisted.orderId());
    }

    @Test
    void replayReturnsCommittedOrderWithoutRepeatingRemoteCalls() {
        Fixture fixture = fixture(true);
        UUID orderId = UUID.randomUUID();
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, SESSION_ID, IDEMPOTENCY_KEY, "a".repeat(64), UUID.randomUUID(), Instant.now());
        saga.markOrderCreated(orderId, Instant.now());
        CustomerOrder order = CustomerOrder.create(
                orderId, "ORD-EXISTING", SESSION_ID, CUSTOMER_ID, OrderStatus.PENDING_PAYMENT,
                PaymentTiming.PREPAID, PaymentMethod.VNPAY, 100, 0, 100, 0, 0, 0, 0, 100,
                null, Instant.now());
        when(fixture.sagas.findById(SAGA_ID)).thenReturn(Optional.of(saga));
        when(fixture.orders.findByIdAndCustomerId(orderId, CUSTOMER_ID)).thenReturn(Optional.of(order));

        var result = fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);

        assertTrue(result.replay());
        assertEquals(orderId, result.orderId());
        verify(fixture.revalidation, never()).revalidate(any(), any());
        verify(fixture.reservations, never()).reserve(any());
        verify(fixture.quoteConsumption, never()).consume(any());
        verify(fixture.persistence, never()).persist(any(), any());
        verify(fixture.paymentCreation).ensurePayment(any());
        verify(fixture.reservationFinalization).finalizeIfConfirmed(SAGA_ID, orderId);
    }

    @Test
    void quoteConsumptionFailureCompensatesBothReservations() {
        Fixture fixture = fixture(false);
        OrderException failure = new OrderException(HttpStatus.CONFLICT, "QUOTE_EXPIRED", "expired");
        Mockito.doThrow(failure).when(fixture.quoteConsumption).consume(fixture.input);

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals(failure, thrown);
        verify(fixture.reservations).compensateAfterFailure(fixture.input, fixture.reserved, failure);
        verify(fixture.persistence, never()).persist(any(), any());
        verify(fixture.paymentCreation, never()).ensurePayment(any());
        verify(fixture.reservationFinalization, never()).finalizeIfConfirmed(any(), any());
    }

    @Test
    void persistenceFailureKeepsReservationsForIdempotentRetry() {
        Fixture fixture = fixture(false);
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(fixture.persistence.persist(fixture.input, fixture.reserved)).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals(failure, thrown);
        verify(fixture.reservations, never()).compensateAfterFailure(any(), any(), any());
        verify(fixture.paymentCreation, never()).ensurePayment(any());
        verify(fixture.reservationFinalization, never()).finalizeIfConfirmed(any(), any());
    }

    @Test
    void paymentFailureAfterOrderCommitDoesNotCompensateCommittedOrder() {
        Fixture fixture = fixture(false);
        OrderException failure = new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_SERVICE_UNAVAILABLE", "down");
        Mockito.doThrow(failure).when(fixture.paymentCreation).ensurePayment(any());

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals(failure, thrown);
        verify(fixture.persistence).persist(fixture.input, fixture.reserved);
        verify(fixture.reservations, never()).compensateAfterFailure(any(), any(), any());
        verify(fixture.reservationFinalization, never()).finalizeIfConfirmed(any(), any());
    }

    @Test
    void finalizationFailureAfterPaymentCheckpointDoesNotReleaseCommittedReservations() {
        Fixture fixture = fixture(false);
        OrderException failure = new OrderException(
                HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_COMMIT_UNAVAILABLE", "down");
        Mockito.doThrow(failure).when(fixture.reservationFinalization)
                .finalizeIfConfirmed(SAGA_ID, fixture.persisted.orderId());

        OrderException thrown = assertThrows(OrderException.class,
                () -> fixture.service.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY));

        assertEquals(failure, thrown);
        verify(fixture.paymentCreation).ensurePayment(any());
        verify(fixture.reservations, never()).compensateAfterFailure(any(), any(), any());
    }

    private Fixture fixture(boolean replay) {
        OrderCreationAdmissionService admissions = Mockito.mock(OrderCreationAdmissionService.class);
        OrderCreationRevalidationService revalidation = Mockito.mock(OrderCreationRevalidationService.class);
        OrderReservationService reservations = Mockito.mock(OrderReservationService.class);
        ShippingQuoteConsumptionService quoteConsumption = Mockito.mock(ShippingQuoteConsumptionService.class);
        OrderCreationPersistenceService persistence = Mockito.mock(OrderCreationPersistenceService.class);
        OrderPaymentCreationService paymentCreation = Mockito.mock(OrderPaymentCreationService.class);
        OrderReservationFinalizationService reservationFinalization =
                Mockito.mock(OrderReservationFinalizationService.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        CustomerOrderRepository orders = Mockito.mock(CustomerOrderRepository.class);
        ValidatedOrderInput input = Mockito.mock(ValidatedOrderInput.class);
        CreationContext context = Mockito.mock(CreationContext.class);
        PricingBreakdown pricing = Mockito.mock(PricingBreakdown.class);
        ReservationResult reserved = new ReservationResult(UUID.randomUUID(), UUID.randomUUID());
        PersistedOrder persisted = new PersistedOrder(
                UUID.randomUUID(), "ORD-NEW", OrderStatus.PENDING_PAYMENT, false);
        when(admissions.admit(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY))
                .thenReturn(new AdmissionResult(SAGA_ID, UUID.randomUUID(), replay));
        when(revalidation.revalidate(CUSTOMER_ID, SAGA_ID)).thenReturn(input);
        when(input.context()).thenReturn(context);
        when(input.pricing()).thenReturn(pricing);
        when(context.customerId()).thenReturn(CUSTOMER_ID);
        when(context.paymentTiming()).thenReturn(PaymentTiming.PREPAID);
        when(context.paymentMethod()).thenReturn(PaymentMethod.VNPAY);
        when(context.correlationId()).thenReturn(UUID.randomUUID());
        when(pricing.finalTotalVnd()).thenReturn(100L);
        when(reservations.reserve(input)).thenReturn(reserved);
        when(persistence.persist(input, reserved)).thenReturn(persisted);
        when(paymentCreation.ensurePayment(any()))
                .thenReturn(new PaymentCheckpoint(UUID.randomUUID(), Instant.now().plusSeconds(900),
                        "https://pay.example/checkout", false));
        return new Fixture(
                input, reserved, persisted, revalidation, reservations, quoteConsumption, persistence,
                paymentCreation, reservationFinalization, sagas, orders,
                new OrderCreationOrchestrator(
                        admissions, revalidation, reservations, quoteConsumption, persistence,
                        paymentCreation, reservationFinalization, sagas, orders));
    }

    private record Fixture(
            ValidatedOrderInput input,
            ReservationResult reserved,
            PersistedOrder persisted,
            OrderCreationRevalidationService revalidation,
            OrderReservationService reservations,
            ShippingQuoteConsumptionService quoteConsumption,
            OrderCreationPersistenceService persistence,
            OrderPaymentCreationService paymentCreation,
            OrderReservationFinalizationService reservationFinalization,
            OrderSagaRepository sagas,
            CustomerOrderRepository orders,
            OrderCreationOrchestrator service) {
    }
}
