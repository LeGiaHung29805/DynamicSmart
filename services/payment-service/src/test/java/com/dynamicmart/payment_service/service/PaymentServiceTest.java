package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.dto.request.OrderPaymentContextRequest;
import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.entity.Payment;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.outbox.OutboxEventRepository;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private PaymentRepository payments;
    private PaymentAttemptRepository attempts;
    private OutboxEventRepository outbox;
    private PaymentGatewayRouter gateways;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        attempts = mock(PaymentAttemptRepository.class);
        outbox = mock(OutboxEventRepository.class);
        gateways = mock(PaymentGatewayRouter.class);
        service = new PaymentService(payments, attempts, outbox, gateways, new ObjectMapper());
    }

    @Test
    void rejectsPrepaidCod() {
        var request = new OrderPaymentContextRequest(UUID.randomUUID(), UUID.randomUUID(), 100_000L,
                OrderPaymentContextRequest.PaymentTiming.PREPAID, OrderPaymentContextRequest.PaymentMethod.COD, UUID.randomUUID());

        PaymentException error = assertThrows(PaymentException.class, () -> service.createForOrder(request));

        assertEquals("PREPAID_COD_FORBIDDEN", error.getCode());
        verify(payments, never()).save(any());
    }

    @Test
    void customerReceivedConfirmationMarksCodPaidAndDuplicateIsIdempotent() {
        Payment payment = payment("POSTPAID", "COD", "PENDING");
        when(payments.findByOrderIdForUpdate(payment.getOrderId())).thenReturn(Optional.of(payment));
        when(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId())).thenReturn(List.of());

        var first = service.collectCodForReceivedOrder(payment.getOrderId());
        var second = service.collectCodForReceivedOrder(payment.getOrderId());

        assertEquals("PAID", first.status());
        assertEquals("PAID", second.status());
        verify(attempts, times(1)).save(any(PaymentAttempt.class));
        verify(outbox, times(1)).save(any());
    }

    @Test
    void rejectsReceivedConfirmationForNonCodPayment() {
        Payment payment = payment("POSTPAID", "VNPAY", "PENDING");
        when(payments.findByOrderIdForUpdate(payment.getOrderId())).thenReturn(Optional.of(payment));

        PaymentException error = assertThrows(PaymentException.class,
                () -> service.collectCodForReceivedOrder(payment.getOrderId()));

        assertEquals("PAYMENT_NOT_COD", error.getCode());
    }

    @Test
    void postpaidVnPayFailureEndsOnlyAttempt() {
        Payment payment = payment("POSTPAID", "VNPAY", "PENDING");
        PaymentAttempt attempt = attempt(payment);
        when(attempts.findByProviderReferenceForUpdate("REF-1")).thenReturn(Optional.of(attempt));
        when(payments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        var outcome = service.applyVnPayFailure("REF-1", payment.getAmountVnd());

        assertEquals(PaymentService.CallbackOutcome.APPLIED, outcome);
        assertEquals("PENDING", payment.getStatus());
        assertEquals("FAILED", attempt.getStatus());
        verify(outbox, never()).save(any());
    }

    @Test
    void returnStatusRequiresPaymentOwnership() {
        Payment payment = payment("PREPAID", "VNPAY", "PENDING"); PaymentAttempt attempt = attempt(payment);
        when(attempts.findByProviderReference("REF-1")).thenReturn(Optional.of(attempt));
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentException error = assertThrows(PaymentException.class,
                () -> service.getByVnPayReference("REF-1", UUID.randomUUID()));

        assertEquals("PAYMENT_OWNERSHIP_DENIED", error.getCode());
    }

    @Test
    void createsPrepaidZaloPayAttemptThroughProviderRouter() {
        UUID orderId = UUID.randomUUID();
        var request = new OrderPaymentContextRequest(orderId, UUID.randomUUID(), 100_000L,
                OrderPaymentContextRequest.PaymentTiming.PREPAID, OrderPaymentContextRequest.PaymentMethod.ZALOPAY, UUID.randomUUID());
        when(payments.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(attempts.findByPaymentIdOrderByAttemptNoDesc(any())).thenReturn(List.of());
        when(gateways.create(eq("ZALOPAY"), any(), eq(orderId), eq(request.customerId()), anyLong(), anyInt(), any()))
                .thenReturn(new PaymentGatewayRouter.CreatedPayment("ZP-REF", "https://zalopay.test/pay"));

        var response = service.createForOrder(request);

        assertEquals("ZALOPAY", response.method());
        assertEquals("https://zalopay.test/pay", response.redirectUrl());
        verify(attempts).save(any(PaymentAttempt.class));
    }

    @Test
    void orderRetryDoesNotReturnAnExpiredProviderUrl() {
        Payment payment = payment("PREPAID", "VNPAY", "PENDING");
        payment.setExpiresAt(Instant.now().plusSeconds(600));
        PaymentAttempt expired = attempt(payment);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        expired.setRedirectUrl("https://provider.test/expired");
        var request = new OrderPaymentContextRequest(payment.getOrderId(), payment.getCustomerId(), payment.getAmountVnd(),
                OrderPaymentContextRequest.PaymentTiming.PREPAID, OrderPaymentContextRequest.PaymentMethod.VNPAY,
                payment.getCorrelationId());
        when(payments.findByOrderId(payment.getOrderId())).thenReturn(Optional.of(payment));
        when(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId())).thenReturn(List.of(expired));
        when(gateways.create(eq("VNPAY"), eq(payment.getId()), eq(payment.getOrderId()), eq(payment.getCustomerId()),
                eq(payment.getAmountVnd()), eq(2), any()))
                .thenReturn(new PaymentGatewayRouter.CreatedPayment("REF-2", "https://provider.test/fresh"));

        var response = service.createForOrder(request);

        assertEquals("https://provider.test/fresh", response.redirectUrl());
        assertEquals("EXPIRED", expired.getStatus());
        verify(attempts, times(2)).save(any(PaymentAttempt.class));
    }

    @Test
    void orderServiceCanReadLatestPostpaidVnPayAttemptByOrder() {
        Payment payment = payment("POSTPAID", "VNPAY", "PENDING");
        PaymentAttempt attempt = attempt(payment);
        attempt.setRedirectUrl("https://sandbox.vnpayment.vn/payment/demo");
        when(payments.findByOrderId(payment.getOrderId())).thenReturn(Optional.of(payment));
        when(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId())).thenReturn(List.of(attempt));

        var response = service.getByOrderId(payment.getOrderId());

        assertEquals(payment.getId(), response.id());
        assertEquals(attempt.getRedirectUrl(), response.redirectUrl());
    }

    @Test
    void rejectsProviderTransactionAlreadyUsedByAnotherPayment() {
        Payment payment = payment("PREPAID", "VNPAY", "PENDING");
        PaymentAttempt attempt = attempt(payment);
        when(attempts.findByProviderReferenceForUpdate("REF-1")).thenReturn(Optional.of(attempt));
        when(payments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(payments.existsByProviderTransactionRefAndIdNot("VNPAY-TXN-1", payment.getId())).thenReturn(true);

        var outcome = service.applyVnPaySuccess("REF-1", "VNPAY-TXN-1", payment.getAmountVnd());

        assertEquals(PaymentService.CallbackOutcome.DUPLICATE, outcome);
        assertEquals("PENDING", payment.getStatus());
        verify(outbox, never()).save(any());
    }

    private Payment payment(String timing, String method, String status) {
        Payment payment = new Payment(); payment.setId(UUID.randomUUID()); payment.setOrderId(UUID.randomUUID()); payment.setCustomerId(UUID.randomUUID());
        payment.setAmountVnd(125_000L); payment.setTiming(timing); payment.setMethod(method); payment.setStatus(status); payment.setCorrelationId(UUID.randomUUID());
        payment.setCreatedAt(Instant.now()); payment.setUpdatedAt(Instant.now()); return payment;
    }

    private PaymentAttempt attempt(Payment payment) {
        PaymentAttempt attempt = new PaymentAttempt(); attempt.setId(UUID.randomUUID()); attempt.setPaymentId(payment.getId()); attempt.setProviderReference("REF-1");
        attempt.setAmountVnd(payment.getAmountVnd()); attempt.setProvider("VNPAY"); attempt.setStatus("CREATED"); attempt.setCreatedAt(Instant.now()); attempt.setUpdatedAt(Instant.now()); attempt.setExpiresAt(Instant.now().plusSeconds(600)); return attempt;
    }
}
