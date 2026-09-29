package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCallbackServiceTest {
    private ZaloPayGateway zaloPay;
    private PayOsGateway payOs;
    private SePayQrGateway sePay;
    private PaymentAttemptRepository attempts;
    private PaymentCallbackAuditRepository audits;
    private PaymentService payments;
    private ProviderCallbackService service;

    @BeforeEach
    void setUp() {
        zaloPay = mock(ZaloPayGateway.class); payOs = mock(PayOsGateway.class); sePay = mock(SePayQrGateway.class);
        attempts = mock(PaymentAttemptRepository.class); audits = mock(PaymentCallbackAuditRepository.class); payments = mock(PaymentService.class);
        service = new ProviderCallbackService(zaloPay, payOs, sePay, attempts, audits, payments, new ObjectMapper());
    }

    @Test
    void rejectsAndAuditsInvalidZaloPayMac() {
        String data = "{\"app_trans_id\":\"REF-ZP\",\"amount\":100000,\"zp_trans_id\":123}";
        PaymentAttempt attempt = attempt("ZALOPAY", "REF-ZP", 100_000L);
        when(attempts.findByProviderReference("REF-ZP")).thenReturn(Optional.of(attempt));
        when(zaloPay.validCallback(data, "bad")).thenReturn(false);

        Map<String, Object> response = service.zaloPay(Map.of("data", data, "mac", "bad"));

        assertEquals(-1, response.get("return_code"));
        verify(payments, never()).applyVnPaySuccess(any(), any(), any(Long.class));
        ArgumentCaptor<PaymentCallbackAudit> audit = ArgumentCaptor.forClass(PaymentCallbackAudit.class);
        verify(audits).save(audit.capture());
        assertFalse(audit.getValue().isChecksumValid());
    }

    @Test
    void appliesValidPayOsWebhookOnce() throws Exception {
        var body = new ObjectMapper().readTree("{\"success\":true,\"data\":{\"orderCode\":12345,\"amount\":100000,\"reference\":\"TX-PAYOS\",\"code\":\"00\"},\"signature\":\"valid\"}");
        PaymentAttempt attempt = attempt("PAYOS", "12345", 100_000L);
        when(attempts.findByProviderReference("12345")).thenReturn(Optional.of(attempt));
        when(payOs.validWebhook(body.path("data"), "valid")).thenReturn(true);
        when(payments.applyVnPaySuccess("12345", "TX-PAYOS", 100_000L)).thenReturn(PaymentService.CallbackOutcome.APPLIED);

        Map<String, Object> response = service.payOs(body);

        assertEquals(true, response.get("success"));
        verify(payments).applyVnPaySuccess("12345", "TX-PAYOS", 100_000L);
    }

    private PaymentAttempt attempt(String provider, String reference, long amount) {
        PaymentAttempt attempt = new PaymentAttempt(); attempt.setId(UUID.randomUUID()); attempt.setPaymentId(UUID.randomUUID());
        attempt.setProvider(provider); attempt.setProviderReference(reference); attempt.setAmountVnd(amount); return attempt;
    }
}
