package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import java.util.LinkedHashMap;
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

class VnPayCallbackServiceTest {
    private VnPayGateway gateway;
    private PaymentAttemptRepository attempts;
    private PaymentCallbackAuditRepository audits;
    private PaymentService payments;
    private VnPayCallbackService service;

    @BeforeEach
    void setUp() {
        gateway = mock(VnPayGateway.class); attempts = mock(PaymentAttemptRepository.class);
        audits = mock(PaymentCallbackAuditRepository.class); payments = mock(PaymentService.class);
        service = new VnPayCallbackService(gateway, attempts, audits, payments, new ObjectMapper());
    }

    @Test
    void invalidSignatureIsRejectedAndAuditedWithoutBusinessEffect() {
        Map<String, String> request = Map.of("vnp_TxnRef", "UNKNOWN", "vnp_Amount", "10000000", "vnp_SecureHash", "secret");
        when(gateway.hasValidSignature(request)).thenReturn(false);
        when(attempts.findByProviderReference("UNKNOWN")).thenReturn(Optional.empty());

        Map<String, String> response = service.process(request);

        assertEquals("97", response.get("RspCode"));
        ArgumentCaptor<PaymentCallbackAudit> audit = ArgumentCaptor.forClass(PaymentCallbackAudit.class); verify(audits).save(audit.capture());
        assertEquals("REJECTED", audit.getValue().getProcessedResult()); assertFalse(audit.getValue().getRawPayload().contains("vnp_SecureHash"));
        verify(payments, never()).applyVnPaySuccess(any(), any(), any(Long.class));
    }

    @Test
    void lateSignedCallbackIsAcknowledgedAsLateAudit() {
        PaymentAttempt attempt = new PaymentAttempt(); attempt.setPaymentId(UUID.randomUUID()); attempt.setAmountVnd(100_000L);
        Map<String, String> request = new LinkedHashMap<>(); request.put("vnp_TxnRef", "REF-1"); request.put("vnp_Amount", "10000000");
        request.put("vnp_ResponseCode", "00"); request.put("vnp_TransactionStatus", "00"); request.put("vnp_TransactionNo", "TX-1"); request.put("vnp_SecureHash", "valid");
        when(gateway.hasValidSignature(request)).thenReturn(true); when(attempts.findByProviderReference("REF-1")).thenReturn(Optional.of(attempt));
        when(payments.applyVnPaySuccess("REF-1", "TX-1", 100_000L)).thenReturn(PaymentService.CallbackOutcome.LATE_AUDIT);

        Map<String, String> response = service.process(request);

        assertEquals("02", response.get("RspCode"));
        ArgumentCaptor<PaymentCallbackAudit> audit = ArgumentCaptor.forClass(PaymentCallbackAudit.class); verify(audits).save(audit.capture());
        assertEquals("LATE_AUDIT", audit.getValue().getProcessedResult());
    }
}
