package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.PaymentClient.OrderPaymentContextRequest;
import com.dynamicmart.order_service.client.PaymentClient.PaymentResponse;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.SagaStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.service.OrderPaymentCreationService.PaymentCommand;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class OrderPaymentCreationServiceTests {
    private static final UUID SAGA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID CORRELATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final Instant DUE_AT = Instant.parse("2026-09-24T08:00:00Z");

    @Test
    void createsPaymentContextAndPersistsReturnedPaymentId() {
        Fixture fixture = fixture();
        when(fixture.payments.createPayment(any())).thenReturn(response(125_000));

        fixture.service.ensurePayment(command(125_000, SagaStatus.ORDER_CREATED, null, "ORDER_SNAPSHOTS_PERSISTED"));

        ArgumentCaptor<OrderPaymentContextRequest> request = ArgumentCaptor.forClass(OrderPaymentContextRequest.class);
        verify(fixture.payments).createPayment(request.capture());
        assertEquals(ORDER_ID, request.getValue().orderId());
        assertEquals(CUSTOMER_ID, request.getValue().customerId());
        assertEquals(125_000, request.getValue().amountVnd());
        verify(fixture.checkpoints).markPaymentRequested(SAGA_ID, ORDER_ID, PAYMENT_ID, DUE_AT);
    }

    @Test
    void rejectsPaymentContextThatDoesNotEchoAuthoritativeAmount() {
        Fixture fixture = fixture();
        when(fixture.payments.createPayment(any())).thenReturn(response(124_000));

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.ensurePayment(
                command(125_000, SagaStatus.ORDER_CREATED, null, "ORDER_SNAPSHOTS_PERSISTED")));

        assertEquals("PAYMENT_CONTEXT_MISMATCH", exception.getCode());
        verify(fixture.checkpoints, never()).markPaymentRequested(any(), any(), any(), any());
    }

    @Test
    void paymentRequestedCheckpointReusesOnlineAttemptForBrowserRedirect() {
        Fixture fixture = fixture();
        when(fixture.payments.createVnPayAttempt(PAYMENT_ID)).thenReturn(response(125_000));

        var result = fixture.service.ensurePayment(command(125_000, SagaStatus.PAYMENT_REQUESTED, PAYMENT_ID,
                "PAYMENT_CONTEXT_CREATED"));

        verify(fixture.payments, never()).createPayment(any());
        verify(fixture.payments).createVnPayAttempt(PAYMENT_ID);
        verify(fixture.checkpoints, never()).markPaymentRequested(any(), any(), any(), any());
        assertEquals("https://pay.example/attempt", result.redirectUrl());
    }

    @Test
    void zeroValueOrderRecordsNoPaymentCheckpointWithoutRemoteCall() {
        Fixture fixture = fixture();

        fixture.service.ensurePayment(new PaymentCommand(
                SAGA_ID, SagaStatus.ORDER_CREATED, "ORDER_SNAPSHOTS_PERSISTED", null, null,
                ORDER_ID, CUSTOMER_ID, 0, PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE, CORRELATION_ID));

        verify(fixture.payments, never()).createPayment(any());
        verify(fixture.checkpoints).markPaymentNotRequired(SAGA_ID, ORDER_ID);
    }

    @Test
    void incompletePaymentRequestedCheckpointIsRejected() {
        Fixture fixture = fixture();

        OrderException exception = assertThrows(OrderException.class, () -> fixture.service.ensurePayment(
                command(125_000, SagaStatus.PAYMENT_REQUESTED, null, "PAYMENT_CONTEXT_CREATED")));

        assertEquals("PAYMENT_CHECKPOINT_INCOMPLETE", exception.getCode());
    }

    @Test
    void reservationFinalizationCheckpointMakesPaymentRetryRemoteCallFree() {
        Fixture fixture = fixture();

        var result = fixture.service.ensurePayment(command(
                125_000, SagaStatus.FINALIZING_RESERVATIONS, PAYMENT_ID,
                "INVENTORY_COMMIT_RETRY_REQUIRED"));

        assertEquals(PAYMENT_ID, result.paymentId());
        verify(fixture.payments, never()).createPayment(any());
    }

    private Fixture fixture() {
        PaymentClient payments = Mockito.mock(PaymentClient.class);
        OrderPaymentCheckpointService checkpoints = Mockito.mock(OrderPaymentCheckpointService.class);
        return new Fixture(payments, checkpoints, new OrderPaymentCreationService(payments, checkpoints));
    }

    private PaymentCommand command(long amount, SagaStatus status, UUID paymentId, String step) {
        return new PaymentCommand(
                SAGA_ID, status, step, paymentId, paymentId == null ? null : DUE_AT,
                ORDER_ID, CUSTOMER_ID, amount,
                PaymentTiming.PREPAID, PaymentMethod.VNPAY, CORRELATION_ID);
    }

    private PaymentResponse response(long amount) {
        return new PaymentResponse(
                PAYMENT_ID, ORDER_ID, amount, "PREPAID", "VNPAY", "PENDING",
                "https://pay.example/attempt", DUE_AT, null);
    }

    private record Fixture(
            PaymentClient payments,
            OrderPaymentCheckpointService checkpoints,
            OrderPaymentCreationService service) {
    }
}
