package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.dto.request.CreateOrderRequest;
import com.dynamicmart.order_service.dto.response.CheckoutSessionResponse;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.service.OrderCreationOrchestrator.CreationResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreateOrderFacadeTests {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CART_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ADDRESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Test
    void compatibilityEndpointBuildsCheckoutThenDelegatesToOwnedOrchestrator() {
        Fixture fixture = fixture();
        when(fixture.sagas.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(fixture.sessions.create(any(), any())).thenReturn(session());
        when(fixture.orderCreation.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY))
                .thenReturn(result(false));

        var response = fixture.service.create(CUSTOMER_ID, IDEMPOTENCY_KEY, request());

        assertEquals(ORDER_ID, response.orderId());
        assertEquals(SAGA_ID, response.sagaId());
        verify(fixture.sessions).create(any(), any());
        verify(fixture.sessions).update(any(), any(), any());
        verify(fixture.previews).preview(any(), any(), any());
        verify(fixture.orderCreation).create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);
    }

    @Test
    void idempotentReplayUsesOriginalSessionWithoutCreatingAnotherCheckout() {
        Fixture fixture = fixture();
        OrderSaga saga = OrderSaga.start(
                SAGA_ID, SESSION_ID, IDEMPOTENCY_KEY, "a".repeat(64), UUID.randomUUID(), Instant.now());
        when(fixture.sagas.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(saga));
        when(fixture.orderCreation.create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY))
                .thenReturn(result(true));

        var response = fixture.service.create(CUSTOMER_ID, IDEMPOTENCY_KEY, request());

        assertTrue(response.replay());
        verify(fixture.sessions, never()).create(any(), any());
        verify(fixture.sessions, never()).update(any(), any(), any());
        verify(fixture.previews, never()).preview(any(), any(), any());
        verify(fixture.orderCreation).create(CUSTOMER_ID, SESSION_ID, IDEMPOTENCY_KEY);
    }

    private Fixture fixture() {
        CheckoutSessionService sessions = Mockito.mock(CheckoutSessionService.class);
        CheckoutPreviewService previews = Mockito.mock(CheckoutPreviewService.class);
        OrderSagaRepository sagas = Mockito.mock(OrderSagaRepository.class);
        OrderCreationOrchestrator orderCreation = Mockito.mock(OrderCreationOrchestrator.class);
        return new Fixture(sessions, previews, sagas, orderCreation,
                new CreateOrderFacade(sessions, previews, sagas, orderCreation));
    }

    private CheckoutSessionResponse session() {
        Instant now = Instant.now();
        return new CheckoutSessionResponse(
                SESSION_ID, CheckoutSource.CART, CART_ID, null, CheckoutStatus.ACTIVE,
                null, null, now.plusSeconds(900), now, now, List.of());
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(
                CART_ID, ADDRESS_ID, null, null, null, PaymentTiming.PREPAID, PaymentMethod.VNPAY);
    }

    private CreationResult result(boolean replay) {
        return new CreationResult(
                ORDER_ID, "ORD-1", OrderStatus.PENDING_PAYMENT, SAGA_ID,
                UUID.randomUUID(), Instant.now().plusSeconds(900), replay);
    }

    private record Fixture(
            CheckoutSessionService sessions,
            CheckoutPreviewService previews,
            OrderSagaRepository sagas,
            OrderCreationOrchestrator orderCreation,
            CreateOrderFacade service) {
    }
}
