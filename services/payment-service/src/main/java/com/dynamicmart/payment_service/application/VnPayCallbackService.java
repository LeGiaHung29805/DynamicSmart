package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VnPayCallbackService {
    private final VnPayGateway vnPay;
    private final PaymentAttemptRepository attempts;
    private final PaymentCallbackAuditRepository audits;
    private final PaymentService payments;
    private final ObjectMapper objectMapper;

    public VnPayCallbackService(VnPayGateway vnPay, PaymentAttemptRepository attempts, PaymentCallbackAuditRepository audits, PaymentService payments, ObjectMapper objectMapper) {
        this.vnPay = vnPay; this.attempts = attempts; this.audits = audits; this.payments = payments; this.objectMapper = objectMapper;
    }

    /** VNPay IPN acknowledgement. All requests are audited, including invalid and duplicate callbacks. */
    @Transactional
    public Map<String, String> process(Map<String, String> request) {
        String reference = request.get("vnp_TxnRef");
        var attempt = reference == null ? java.util.Optional.<com.dynamicmart.payment_service.entity.PaymentAttempt>empty() : attempts.findByProviderReference(reference);
        boolean signatureValid = vnPay.hasValidSignature(request);
        long amount = parseAmount(request.get("vnp_Amount"));
        boolean amountValid = attempt.isPresent() && amount > 0 && amount == attempt.get().getAmountVnd();
        String result = "REJECTED";
        if (signatureValid && amountValid) {
            boolean applied = "00".equals(request.get("vnp_ResponseCode"))
                    ? payments.applyVnPaySuccess(reference, amount)
                    : payments.applyVnPayFailure(reference, amount);
            result = applied ? "APPLIED" : "DUPLICATE";
        }
        PaymentCallbackAudit audit = new PaymentCallbackAudit(); audit.setId(UUID.randomUUID()); audit.setPaymentId(attempt.map(a -> a.getPaymentId()).orElse(null)); audit.setProvider("VNPAY"); audit.setProviderTransactionRef(reference); audit.setRawPayload(safePayload(request)); audit.setChecksumValid(signatureValid); audit.setAmountValid(amountValid); audit.setProcessedResult(result); audit.setReceivedAt(Instant.now()); audits.save(audit);
        return "APPLIED".equals(result) || "DUPLICATE".equals(result)
                ? Map.of("RspCode", "00", "Message", "Confirm Success")
                : Map.of("RspCode", "97", "Message", "Invalid request");
    }

    private long parseAmount(String value) { try { long vnpayAmount = Long.parseLong(value); return vnpayAmount > 0 && vnpayAmount % 100 == 0 ? vnpayAmount / 100 : -1; } catch (RuntimeException ignored) { return -1; } }
    private String safePayload(Map<String, String> request) {
        Map<String, String> safe = new LinkedHashMap<>(request); safe.remove("vnp_SecureHash"); safe.remove("vnp_CardNumber"); safe.remove("vnp_BankTranNo");
        try { return objectMapper.writeValueAsString(safe); } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot audit VNPay callback", exception); }
    }
}
