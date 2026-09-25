package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.CodConfirmationRequest;
import com.dynamicmart.payment_service.api.OrderPaymentContextRequest;
import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.entity.Payment;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.repository.OutboxEventRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private PaymentRepository payments;
    private PaymentAttemptRepository attempts;
    private OutboxEventRepository outbox;
    private VnPayGateway vnPay;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentRepository.class);
        attempts = mock(PaymentAttemptRepository.class);
        outbox = mock(OutboxEventRepository.class);
        vnPay = mock(VnPayGateway.class);
        service = new PaymentService(payments, attempts, outbox, vnPay, new ObjectMapper());
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
    void codStaysPendingUntilAuditedConfirmationAndDuplicateIsIdempotent() {
        Payment payment = payment("POSTPAID", "COD", "PENDING");
        UUID adminId = UUID.randomUUID();
        var request = new CodConfirmationRequest(payment.getAmountVnd(), "COD-RECEIPT-1");
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId())).thenReturn(List.of());

        var first = service.confirmCod(payment.getId(), request, adminId);
        var second = service.confirmCod(payment.getId(), request, adminId);

        assertEquals("PAID", first.status());
        assertEquals("COD-RECEIPT-1", second.codReceiptNo());
        assertEquals(adminId, second.codConfirmedBy());
        verify(attempts, times(1)).save(any(PaymentAttempt.class));
        verify(outbox, times(1)).save(any());
    }

    @Test
    void rejectsDifferentCodConfirmationAfterPaymentWasPaid() {
        Payment payment = payment("POSTPAID", "COD", "PAID");
        payment.setCodReceiptNo("ORIGINAL"); payment.setCodConfirmedBy(UUID.randomUUID());
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentException error = assertThrows(PaymentException.class, () -> service.confirmCod(payment.getId(),
                new CodConfirmationRequest(payment.getAmountVnd(), "OTHER"), UUID.randomUUID()));

        assertEquals("COD_CONFIRMATION_CONFLICT", error.getCode());
    }

    @Test
    void postpaidVnPayFailureEndsOnlyAttempt() {
        Payment payment = payment("POSTPAID", "VNPAY", "PENDING");
        PaymentAttempt attempt = attempt(payment);
        when(attempts.findByProviderReference("REF-1")).thenReturn(Optional.of(attempt));
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

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
