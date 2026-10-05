package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.PayOsGateway;
import com.dynamicmart.payment_service.client.SePayQrGateway;
import com.dynamicmart.payment_service.client.ZaloPayGateway;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.entity.PaymentCallbackAudit;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProviderCallbackService {
    private final ZaloPayGateway zaloPay;
    private final PayOsGateway payOs;
    private final SePayQrGateway sePay;
    private final PaymentAttemptRepository attempts;
    private final PaymentCallbackAuditRepository audits;
    private final PaymentService payments;
    private final ObjectMapper objectMapper;

    public ProviderCallbackService(ZaloPayGateway zaloPay, PayOsGateway payOs, SePayQrGateway sePay,
                                   PaymentAttemptRepository attempts, PaymentCallbackAuditRepository audits,
                                   PaymentService payments, ObjectMapper objectMapper) {
        this.zaloPay = zaloPay; this.payOs = payOs; this.sePay = sePay; this.attempts = attempts;
        this.audits = audits; this.payments = payments; this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> zaloPay(Map<String, Object> body) {
        String data = text(body.get("data")); String mac = text(body.get("mac"));
        JsonNode payload = parse(data); String reference = payload.path("app_trans_id").asText("");
        long amount = payload.path("amount").asLong(-1); String transaction = payload.path("zp_trans_id").asText("");
        Optional<PaymentAttempt> attempt = providerAttempt(reference, "ZALOPAY");
        boolean signatureValid = zaloPay.validCallback(data, mac); boolean amountValid = validAmount(attempt, amount);
        String result = applySuccess(reference, transaction, amount, signatureValid, amountValid);
        audit("ZALOPAY", attempt, transaction, signatureValid, amountValid, result, Map.of("appTransId", reference, "amount", amount, "zpTransId", transaction));
        return Map.of("return_code", "APPLIED".equals(result) || "DUPLICATE".equals(result) ? 1 : -1,
                "return_message", "APPLIED".equals(result) || "DUPLICATE".equals(result) ? "success" : "invalid callback");
    }

    @Transactional
    public Map<String, Object> payOs(JsonNode body) {
        JsonNode data = body == null ? null : body.path("data"); String signature = body == null ? "" : body.path("signature").asText("");
        String reference = data == null ? "" : data.path("orderCode").asText(""); long amount = data == null ? -1 : data.path("amount").asLong(-1);
        String transaction = data == null ? "" : data.path("reference").asText(""); Optional<PaymentAttempt> attempt = providerAttempt(reference, "PAYOS");
        boolean signatureValid = payOs.validWebhook(data, signature); boolean amountValid = validAmount(attempt, amount);
        boolean success = body != null && body.path("success").asBoolean(false) && "00".equals(data.path("code").asText(""));
        String result = success ? applySuccess(reference, transaction, amount, signatureValid, amountValid) : "REJECTED";
        audit("PAYOS", attempt, transaction, signatureValid, amountValid, result, Map.of("orderCode", reference, "amount", amount, "reference", transaction));
        return Map.of("success", "APPLIED".equals(result) || "DUPLICATE".equals(result));
    }

    @Transactional
    public Map<String, Object> sePay(String rawBody, String timestamp, String signature) {
        JsonNode data = parse(rawBody); String reference = data.path("code").asText(""); long amount = data.path("transferAmount").asLong(-1);
        String transaction = data.path("id").asText(data.path("referenceCode").asText("")); Optional<PaymentAttempt> attempt = providerAttempt(reference, "BANK_QR");
        boolean signatureValid = sePay.validWebhook(rawBody, timestamp, signature); boolean amountValid = validAmount(attempt, amount);
        boolean incoming = "in".equalsIgnoreCase(data.path("transferType").asText(""));
        String result = incoming ? applySuccess(reference, transaction, amount, signatureValid, amountValid) : "REJECTED";
        audit("SEPAY", attempt, transaction, signatureValid, amountValid, result, Map.of("code", reference, "amount", amount, "transactionId", transaction));
        return Map.of("success", "APPLIED".equals(result) || "DUPLICATE".equals(result));
    }

    private String applySuccess(String reference, String transaction, long amount, boolean signatureValid, boolean amountValid) {
        if (!signatureValid || !amountValid || reference.isBlank() || transaction.isBlank()) return "REJECTED";
        return payments.applyVnPaySuccess(reference, transaction, amount).name();
    }

    private Optional<PaymentAttempt> providerAttempt(String reference, String provider) {
        if (reference == null || reference.isBlank()) return Optional.empty();
        return attempts.findByProviderReference(reference).filter(attempt -> provider.equals(attempt.getProvider()));
    }

    private boolean validAmount(Optional<PaymentAttempt> attempt, long amount) { return attempt.isPresent() && amount > 0 && attempt.get().getAmountVnd() == amount; }
    private JsonNode parse(String value) { try { return value == null || value.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(value); } catch (Exception ignored) { return objectMapper.createObjectNode(); } }
    private String text(Object value) { return value == null ? "" : value.toString(); }

    private void audit(String provider, Optional<PaymentAttempt> attempt, String transaction, boolean signatureValid,
                       boolean amountValid, String result, Map<String, Object> safePayload) {
        PaymentCallbackAudit audit = new PaymentCallbackAudit(); audit.setId(UUID.randomUUID());
        audit.setPaymentId(attempt.map(PaymentAttempt::getPaymentId).orElse(null)); audit.setProvider(provider);
        audit.setProviderTransactionRef(transaction); audit.setRawPayload(write(safePayload)); audit.setChecksumValid(signatureValid);
        audit.setAmountValid(amountValid); audit.setProcessedResult(result); audit.setReceivedAt(Instant.now()); audits.save(audit);
    }

    private String write(Map<String, Object> value) { try { return objectMapper.writeValueAsString(new LinkedHashMap<>(value)); } catch (Exception exception) { return "{}"; } }
}
