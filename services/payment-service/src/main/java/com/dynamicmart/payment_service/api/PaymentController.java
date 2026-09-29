package com.dynamicmart.payment_service.api;

import com.dynamicmart.payment_service.application.PaymentService;
import com.dynamicmart.payment_service.application.VnPayCallbackService;
import com.dynamicmart.payment_service.application.InternalApiVerifier;
import com.dynamicmart.payment_service.application.ProviderCallbackService;
import com.dynamicmart.payment_service.repository.PaymentCallbackAuditRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final VnPayCallbackService callbacks;
    private final PaymentCallbackAuditRepository audits;
    private final InternalApiVerifier internalApi;
    private final ProviderCallbackService providerCallbacks;

    public PaymentController(PaymentService paymentService, VnPayCallbackService callbacks, PaymentCallbackAuditRepository audits, InternalApiVerifier internalApi, ProviderCallbackService providerCallbacks) {
        this.paymentService = paymentService; this.callbacks = callbacks; this.audits = audits; this.internalApi = internalApi; this.providerCallbacks = providerCallbacks;
    }

    /** Internal order-service API. Gateway/service authentication must protect this route in deployment. */
    @PostMapping("/order-context")
    public PaymentResponse createForOrder(@RequestHeader("X-Internal-Api-Key") String apiKey, @Valid @RequestBody OrderPaymentContextRequest request) { internalApi.require(apiKey); return paymentService.createForOrder(request); }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID paymentId, @RequestParam(required = false) UUID customerId) { internalApi.require(apiKey); return paymentService.get(paymentId, customerId); }

    /** Internal Order Service read model, including the latest active VNPay redirect for postpaid handover. */
    @GetMapping("/orders/{orderId}")
    public PaymentResponse getByOrder(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID orderId) { internalApi.require(apiKey); return paymentService.getByOrderId(orderId); }

    /** Customer-facing authoritative state for the browser return page; TxnRef alone is never sufficient without JWT ownership. */
    @GetMapping("/vnpay/return-status")
    public PaymentResponse returnStatus(@RequestHeader("X-Authenticated-User-Id") UUID customerId, @RequestParam("vnp_TxnRef") String reference) {
        return paymentService.getByVnPayReference(reference, customerId);
    }

    @GetMapping("/return-status")
    public PaymentResponse providerReturnStatus(@RequestHeader("X-Authenticated-User-Id") UUID customerId, @RequestParam String reference) {
        return paymentService.getByVnPayReference(reference, customerId);
    }

    /** Admin read model. Gateway enforces ADMIN before forwarding this route. */
    @GetMapping
    public PaymentPageResponse list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page); int safeSize = Math.max(1, Math.min(size, 100));
        return paymentService.list(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/{paymentId}/vnpay-attempt")
    public PaymentResponse newVnPayAttempt(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID paymentId) { internalApi.require(apiKey); return paymentService.createVnPayAttempt(paymentId); }

    @PostMapping("/{paymentId}/attempts")
    public PaymentResponse newOnlineAttempt(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID paymentId) { internalApi.require(apiKey); return paymentService.createVnPayAttempt(paymentId); }

    /** Admin-only command; Gateway must inject/validate the admin identity before this reaches the service. */
    @PostMapping("/{paymentId}/cod-confirmations")
    public PaymentResponse confirmCod(@RequestHeader("X-Authenticated-User-Id") UUID adminId, @PathVariable UUID paymentId, @Valid @RequestBody CodConfirmationRequest request) { return paymentService.confirmCod(paymentId, request, adminId); }

    @GetMapping("/{paymentId}/attempts")
    public List<PaymentAttemptResponse> attempts(@PathVariable UUID paymentId) { return paymentService.attempts(paymentId); }

    @GetMapping("/{paymentId}/callback-audits")
    public List<PaymentCallbackAuditResponse> callbackAudits(@PathVariable UUID paymentId) {
        return audits.findTop100ByPaymentIdOrderByReceivedAtDesc(paymentId).stream().map(a -> new PaymentCallbackAuditResponse(a.getId(), a.getProviderTransactionRef(), a.isChecksumValid(), a.isAmountValid(), a.getProcessedResult(), a.getReceivedAt())).toList();
    }

    /** Provider callback endpoint; it intentionally returns a generic acknowledgement and never exposes audit payloads. */
    @RequestMapping(value = "/vnpay/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> request) { return callbacks.process(request); }

    @PostMapping("/zalopay/callback")
    public Map<String, Object> zaloPayCallback(@RequestBody Map<String, Object> request) { return providerCallbacks.zaloPay(request); }

    @PostMapping("/payos/webhook")
    public Map<String, Object> payOsWebhook(@RequestBody JsonNode request) { return providerCallbacks.payOs(request); }

    @PostMapping("/sepay/webhook")
    public Map<String, Object> sePayWebhook(@RequestHeader("X-SePay-Timestamp") String timestamp,
                                            @RequestHeader("X-SePay-Signature") String signature,
                                            @RequestBody String rawBody) {
        return providerCallbacks.sePay(rawBody, timestamp, signature);
    }
}
