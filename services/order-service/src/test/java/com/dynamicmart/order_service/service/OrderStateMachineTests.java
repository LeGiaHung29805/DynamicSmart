package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dynamicmart.order_service.entity.OrderStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderStateMachineTests {
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    @Test
    void prepaidVnPayStartsPendingAndPaymentSuccessConfirms() {
        OrderStatus initial = stateMachine.initialStatus(PaymentTiming.PREPAID, PaymentMethod.VNPAY, 120_000);

        assertEquals(OrderStatus.PENDING_PAYMENT, initial);
        assertIterableEquals(
                List.of(OrderStatus.CONFIRMED),
                stateMachine.onPaymentSucceeded(initial, PaymentTiming.PREPAID, PaymentMethod.VNPAY));
    }

    @Test
    void prepaidPaymentFailureCancelsOnlyWhilePending() {
        assertEquals(
                OrderStatus.CANCELLED,
                stateMachine.onPaymentFailedOrExpired(OrderStatus.PENDING_PAYMENT, PaymentTiming.PREPAID).orElseThrow());
        assertTrue(stateMachine.onPaymentFailedOrExpired(OrderStatus.CONFIRMED, PaymentTiming.PREPAID).isEmpty());
        assertTrue(stateMachine.onPaymentSucceeded(
                OrderStatus.CANCELLED, PaymentTiming.PREPAID, PaymentMethod.VNPAY).isEmpty());
    }

    @Test
    void adminCannotSkipPackingOrHandoverPrepaidOrder() {
        assertInvalid(() -> stateMachine.adminShip(OrderStatus.CONFIRMED));
        assertInvalid(() -> stateMachine.adminHandover(OrderStatus.SHIPPING, PaymentTiming.PREPAID));
    }

    @Test
    void prepaidCustomerReceiptRecordsDeliveredThenCompleted() {
        assertIterableEquals(
                List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                stateMachine.customerConfirmReceived(OrderStatus.SHIPPING, PaymentTiming.PREPAID, true));
    }

    @Test
    void postpaidReceiptBeforePaymentWaitsForPayment() {
        assertEquals(
                OrderStatus.HANDOVER_PENDING,
                stateMachine.adminHandover(OrderStatus.SHIPPING, PaymentTiming.POSTPAID));
        assertIterableEquals(
                List.of(OrderStatus.DELIVERED),
                stateMachine.customerConfirmReceived(
                        OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, false));
        assertIterableEquals(
                List.of(OrderStatus.COMPLETED),
                stateMachine.onPaymentSucceeded(
                        OrderStatus.DELIVERED, PaymentTiming.POSTPAID, PaymentMethod.COD));
    }

    @Test
    void postpaidPaymentBeforeReceiptCompletesDuringReceiptCommand() {
        assertTrue(stateMachine.onPaymentSucceeded(
                OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, PaymentMethod.VNPAY).isEmpty());
        assertIterableEquals(
                List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED),
                stateMachine.customerConfirmReceived(
                        OrderStatus.HANDOVER_PENDING, PaymentTiming.POSTPAID, true));
    }

    @Test
    void freeOrderStartsConfirmedAndNeverAcceptsExternalPayment() {
        assertEquals(
                OrderStatus.CONFIRMED,
                stateMachine.initialStatus(PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE, 0));
        assertInvalid(() -> stateMachine.onPaymentSucceeded(
                OrderStatus.CONFIRMED, PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE));
    }

    @Test
    void invalidPaymentCombinationsAreRejected() {
        assertInvalid(() -> stateMachine.initialStatus(PaymentTiming.PREPAID, PaymentMethod.COD, 100_000));
        assertInvalid(() -> stateMachine.initialStatus(PaymentTiming.NOT_REQUIRED, PaymentMethod.FREE, 100_000));
        assertInvalid(() -> stateMachine.initialStatus(PaymentTiming.POSTPAID, PaymentMethod.COD, 0));
        assertInvalid(() -> stateMachine.initialStatus(PaymentTiming.POSTPAID, PaymentMethod.COD, -1));
    }

    @Test
    void duplicateReceiptAfterCompletionHasNoTransition() {
        assertTrue(stateMachine.customerConfirmReceived(
                OrderStatus.COMPLETED, PaymentTiming.POSTPAID, true).isEmpty());
    }

    private void assertInvalid(Runnable action) {
        OrderException exception = assertThrows(OrderException.class, action::run);
        assertEquals("INVALID_ORDER_TRANSITION", exception.getCode());
        assertEquals(409, exception.getStatus().value());
    }
}
