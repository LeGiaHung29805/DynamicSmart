package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
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
            boolean success = "00".equals(request.get("vnp_ResponseCode")) && "00".equals(request.get("vnp_TransactionStatus"));
            PaymentService.CallbackOutcome outcome = success
                    ? payments.applyVnPaySuccess(reference, request.get("vnp_TransactionNo"), amount)
                    : payments.applyVnPayFailure(reference, amount);
            result = outcome.name();
        }
        PaymentCallbackAudit audit = new PaymentCallbackAudit(); audit.setId(UUID.randomUUID()); audit.setPaymentId(attempt.map(a -> a.getPaymentId()).orElse(null)); audit.setProvider("VNPAY"); audit.setProviderTransactionRef(request.getOrDefault("vnp_TransactionNo", reference)); audit.setRawPayload(safePayload(request)); audit.setChecksumValid(signatureValid); audit.setAmountValid(amountValid); audit.setProcessedResult(result); audit.setReceivedAt(Instant.now()); audits.save(audit);
        return "APPLIED".equals(result) || "DUPLICATE".equals(result)
                ? Map.of("RspCode", "00", "Message", "Confirm Success")
                : "LATE_AUDIT".equals(result) ? Map.of("RspCode", "02", "Message", "Order already confirmed")
                : Map.of("RspCode", "97", "Message", "Invalid request");
    }

    private long parseAmount(String value) { try { long vnpayAmount = Long.parseLong(value); return vnpayAmount > 0 && vnpayAmount % 100 == 0 ? vnpayAmount / 100 : -1; } catch (RuntimeException ignored) { return -1; } }
    private String safePayload(Map<String, String> request) {
        Map<String, String> safe = new LinkedHashMap<>(request); safe.remove("vnp_SecureHash"); safe.remove("vnp_CardNumber"); safe.remove("vnp_BankTranNo");
        try { return objectMapper.writeValueAsString(safe); } catch (JacksonException exception) { throw new IllegalStateException("Cannot audit VNPay callback", exception); }
    }
}
