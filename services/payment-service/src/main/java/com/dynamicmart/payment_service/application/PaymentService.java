package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.CodConfirmationRequest;
import com.dynamicmart.payment_service.api.OrderPaymentContextRequest;
import com.dynamicmart.payment_service.api.PaymentAttemptResponse;
import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.api.PaymentResponse;
import com.dynamicmart.payment_service.entity.OutboxEvent;
import com.dynamicmart.payment_service.entity.Payment;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.repository.OutboxEventRepository;
import com.dynamicmart.payment_service.repository.PaymentAttemptRepository;
import com.dynamicmart.payment_service.repository.PaymentRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import com.dynamicmart.payment_service.api.PaymentPageResponse;

@Service
public class PaymentService {
    private static final Duration VNPAY_TTL = Duration.ofMinutes(15);
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final OutboxEventRepository outbox;
    private final VnPayGateway vnPay;
    private final ObjectMapper objectMapper;

    public enum CallbackOutcome { APPLIED, DUPLICATE, LATE_AUDIT }

    public PaymentService(PaymentRepository payments, PaymentAttemptRepository attempts, OutboxEventRepository outbox, VnPayGateway vnPay, ObjectMapper objectMapper) {
        this.payments = payments; this.attempts = attempts; this.outbox = outbox; this.vnPay = vnPay; this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse createForOrder(OrderPaymentContextRequest request) {
        validateCombination(request);
        var existing = payments.findByOrderId(request.orderId());
        Payment payment = existing.orElseGet(() -> newPayment(request));
        if (!payment.getCustomerId().equals(request.customerId()) || payment.getAmountVnd() != request.amountVnd() || !payment.getTiming().equals(request.timing().name()) || !payment.getMethod().equals(request.method().name()) || !payment.getCorrelationId().equals(request.correlationId())) {
            throw new PaymentException(HttpStatus.CONFLICT, "ORDER_PAYMENT_CONTEXT_CONFLICT", "Ngữ cảnh thanh toán của đơn đã tồn tại nhưng không khớp.");
        }
        if (existing.isEmpty()) payments.save(payment);
        if (request.timing() == OrderPaymentContextRequest.PaymentTiming.PREPAID && request.method() == OrderPaymentContextRequest.PaymentMethod.VNPAY && "PENDING".equals(payment.getStatus())) {
            PaymentAttempt attempt = attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).stream().findFirst().orElseGet(() -> createVnPayAttempt(payment));
            return response(payment, attempt.getRedirectUrl());
        }
        return response(payment, null);
    }

    @Transactional
    public PaymentResponse createVnPayAttempt(UUID paymentId) {
        Payment payment = requirePayment(paymentId);
        if (!"VNPAY".equals(payment.getMethod())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_METHOD_NOT_VNPAY", "Khoản thanh toán này không dùng VNPay.");
        if (!"PENDING".equals(payment.getStatus())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_PENDING", "Chỉ có thể tạo đường dẫn VNPay cho khoản đang chờ thanh toán.");
        if ("PREPAID".equals(payment.getTiming()) && payment.getExpiresAt().isBefore(Instant.now())) {
            expirePrepaid(payment); throw new PaymentException(HttpStatus.GONE, "PAYMENT_EXPIRED", "Khoản thanh toán trả trước đã hết hạn.");
        }
        PaymentAttempt latest = attempts.findByPaymentIdOrderByAttemptNoDesc(paymentId).stream().findFirst().orElse(null);
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus())) && latest.getExpiresAt() != null && latest.getExpiresAt().isAfter(Instant.now())) return response(payment, latest.getRedirectUrl());
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus()))) {
            latest.setStatus("EXPIRED"); latest.setUpdatedAt(Instant.now()); attempts.save(latest);
        }
        return response(payment, createVnPayAttempt(payment).getRedirectUrl());
    }

    @Transactional
    public PaymentResponse createVnPayAttemptForOrder(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_FOR_ORDER_NOT_FOUND", "Không tìm thấy Payment cho PaymentDue."));
        if (!"POSTPAID".equals(payment.getTiming()) || !"VNPAY".equals(payment.getMethod())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_DUE_NOT_APPLICABLE", "PaymentDue chỉ áp dụng cho VNPay trả sau.");
        return createVnPayAttempt(payment.getId());
    }

    @Transactional
    public PaymentResponse confirmCod(UUID paymentId, CodConfirmationRequest request, UUID confirmedBy) {
        Payment payment = requirePayment(paymentId);
        if (!"COD".equals(payment.getMethod()) || !"POSTPAID".equals(payment.getTiming())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_COD", "Chỉ có thể xác nhận thu tiền cho COD trả sau.");
        if ("PAID".equals(payment.getStatus())) {
            if (payment.getAmountVnd() == request.collectedAmountVnd() && request.receiptNo().equals(payment.getCodReceiptNo()) && confirmedBy.equals(payment.getCodConfirmedBy())) return response(payment, null);
            throw new PaymentException(HttpStatus.CONFLICT, "COD_CONFIRMATION_CONFLICT", "COD đã được xác nhận bằng dữ liệu khác.");
        }
        if (!"PENDING".equals(payment.getStatus()) || payment.getAmountVnd() != request.collectedAmountVnd()) throw new PaymentException(HttpStatus.CONFLICT, "COD_AMOUNT_OR_STATUS_INVALID", "Số tiền thực thu hoặc trạng thái COD không hợp lệ.");
        Instant now = Instant.now();
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID()); attempt.setPaymentId(payment.getId()); attempt.setAttemptNo(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).size() + 1);
        attempt.setProvider("COD"); attempt.setProviderReference(request.receiptNo()); attempt.setAmountVnd(payment.getAmountVnd()); attempt.setStatus("SUCCEEDED"); attempt.setCreatedAt(now); attempt.setUpdatedAt(now);
        attempts.save(attempt);
        payment.setStatus("PAID"); payment.setPaidAt(now); payment.setProviderTransactionRef(request.receiptNo()); payment.setCodReceiptNo(request.receiptNo()); payment.setCodConfirmedBy(confirmedBy); payment.setCodConfirmedAt(now); payment.setUpdatedAt(now); payments.save(payment);
        publishSucceeded(payment); return response(payment, null);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(UUID paymentId, UUID customerId) {
        Payment payment = requirePayment(paymentId);
        if (customerId != null && !payment.getCustomerId().equals(customerId)) throw new PaymentException(HttpStatus.FORBIDDEN, "PAYMENT_OWNERSHIP_DENIED", "Bạn không có quyền xem khoản thanh toán này.");
        String redirect = attempts.findByPaymentIdOrderByAttemptNoDesc(paymentId).stream().findFirst().map(PaymentAttempt::getRedirectUrl).orElse(null);
        return response(payment, redirect);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByVnPayReference(String reference, UUID customerId) {
        PaymentAttempt attempt = attempts.findByProviderReference(reference).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_REFERENCE_NOT_FOUND", "Không tìm thấy mã thanh toán VNPay."));
        Payment payment = requirePayment(attempt.getPaymentId());
        if (!payment.getCustomerId().equals(customerId)) throw new PaymentException(HttpStatus.FORBIDDEN, "PAYMENT_OWNERSHIP_DENIED", "Bạn không có quyền xem khoản thanh toán này.");
        return response(payment, null);
    }

    @Transactional(readOnly = true)
    public PaymentPageResponse list(Pageable pageable) {
        var page = payments.findAll(pageable);
        return new PaymentPageResponse(page.getContent().stream().map(payment -> response(payment, null)).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> attempts(UUID paymentId) {
        requirePayment(paymentId);
        return attempts.findByPaymentIdOrderByAttemptNoDesc(paymentId).stream().map(a -> new PaymentAttemptResponse(a.getId(), a.getAttemptNo(), a.getProvider(), a.getProviderReference(), a.getAmountVnd(), a.getStatus(), a.getExpiresAt(), a.getCreatedAt())).toList();
    }

    @Transactional
    public CallbackOutcome applyVnPaySuccess(String reference, String transactionReference, long amountVnd) {
        PaymentAttempt attempt = attempts.findByProviderReference(reference).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_REFERENCE_NOT_FOUND", "Không tìm thấy mã thanh toán VNPay."));
        Payment payment = requirePayment(attempt.getPaymentId());
        if (attempt.getAmountVnd() != amountVnd || payment.getAmountVnd() != amountVnd) return CallbackOutcome.DUPLICATE;
        if ("PAID".equals(payment.getStatus())) return CallbackOutcome.DUPLICATE;
        if (!"PENDING".equals(payment.getStatus())) return CallbackOutcome.LATE_AUDIT;
        Instant now = Instant.now();
        if (attempt.getExpiresAt() != null && attempt.getExpiresAt().isBefore(now)) {
            attempt.setStatus("EXPIRED"); attempt.setUpdatedAt(now); attempts.save(attempt);
            if ("PREPAID".equals(payment.getTiming())) expirePrepaid(payment);
            return CallbackOutcome.LATE_AUDIT;
        }
        attempt.setStatus("SUCCEEDED"); attempt.setUpdatedAt(now); attempts.save(attempt);
        payment.setStatus("PAID"); payment.setPaidAt(now); payment.setProviderTransactionRef(transactionReference); payment.setUpdatedAt(now); payments.save(payment);
        publishSucceeded(payment); return CallbackOutcome.APPLIED;
    }

    /** A failed provider response only ends a prepaid payment. Postpaid VNPay stays PENDING for a new QR/URL. */
    @Transactional
    public CallbackOutcome applyVnPayFailure(String reference, long amountVnd) {
        PaymentAttempt attempt = attempts.findByProviderReference(reference).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_REFERENCE_NOT_FOUND", "Không tìm thấy mã thanh toán VNPay."));
        Payment payment = requirePayment(attempt.getPaymentId());
        if (attempt.getAmountVnd() != amountVnd || payment.getAmountVnd() != amountVnd) return CallbackOutcome.DUPLICATE;
        if (!"PENDING".equals(payment.getStatus())) return "PREPAID".equals(payment.getTiming()) ? CallbackOutcome.LATE_AUDIT : CallbackOutcome.DUPLICATE;
        if ("FAILED".equals(attempt.getStatus()) || "EXPIRED".equals(attempt.getStatus())) return CallbackOutcome.DUPLICATE;
        Instant now = Instant.now(); attempt.setStatus("FAILED"); attempt.setUpdatedAt(now); attempts.save(attempt);
        if ("PREPAID".equals(payment.getTiming())) { payment.setStatus("FAILED"); payment.setFailedAt(now); payment.setUpdatedAt(now); payments.save(payment); publishFailed(payment); }
        return CallbackOutcome.APPLIED;
    }

    @Transactional
    public void expireDuePrepaidPayments() {
        payments.findByStatusAndTimingAndExpiresAtBefore("PENDING", "PREPAID", Instant.now()).forEach(this::expirePrepaid);
    }

    private Payment newPayment(OrderPaymentContextRequest request) {
        Instant now = Instant.now(); Payment payment = new Payment();
        payment.setId(UUID.randomUUID()); payment.setOrderId(request.orderId()); payment.setCustomerId(request.customerId()); payment.setAmountVnd(request.amountVnd()); payment.setTiming(request.timing().name()); payment.setMethod(request.method().name()); payment.setStatus("PENDING"); payment.setCorrelationId(request.correlationId());
        if (request.timing() == OrderPaymentContextRequest.PaymentTiming.PREPAID && request.method() == OrderPaymentContextRequest.PaymentMethod.VNPAY) payment.setExpiresAt(now.plus(VNPAY_TTL));
        payment.setCreatedAt(now); payment.setUpdatedAt(now); return payment;
    }

    private PaymentAttempt createVnPayAttempt(Payment payment) {
        Instant now = Instant.now(); int attemptNo = attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).size() + 1;
        String reference = "DM" + payment.getId().toString().replace("-", "").substring(0, 20).toUpperCase() + attemptNo;
        PaymentAttempt attempt = new PaymentAttempt(); attempt.setId(UUID.randomUUID()); attempt.setPaymentId(payment.getId()); attempt.setAttemptNo(attemptNo); attempt.setProvider("VNPAY"); attempt.setProviderReference(reference); attempt.setAmountVnd(payment.getAmountVnd()); attempt.setStatus("CREATED");
        Instant expires = now.plus(VNPAY_TTL);
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(expires)) expires = payment.getExpiresAt();
        attempt.setExpiresAt(expires); attempt.setRedirectUrl(vnPay.createRedirectUrl(reference, payment.getAmountVnd(), "Thanh toan don " + payment.getOrderId(), expires)); attempt.setCreatedAt(now); attempt.setUpdatedAt(now); attempts.save(attempt); return attempt;
    }

    private void expirePrepaid(Payment payment) {
        if (!"PREPAID".equals(payment.getTiming()) || !"PENDING".equals(payment.getStatus())) return;
        Instant now = Instant.now(); payment.setStatus("EXPIRED"); payment.setFailedAt(now); payment.setUpdatedAt(now); payments.save(payment);
        attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).stream().filter(attempt -> "CREATED".equals(attempt.getStatus()) || "REDIRECTED".equals(attempt.getStatus())).forEach(attempt -> { attempt.setStatus("EXPIRED"); attempt.setUpdatedAt(now); attempts.save(attempt); });
        outbox.save(newEvent(payment, "PaymentExpired", now));
    }

    private void publishSucceeded(Payment payment) {
        Instant now = Instant.now(); outbox.save(newEvent(payment, "PaymentSucceeded", now));
    }

    private void publishFailed(Payment payment) {
        Instant now = Instant.now(); outbox.save(newEvent(payment, "PaymentFailed", now));
    }

    private OutboxEvent newEvent(Payment payment, String eventType, Instant now) {
        OutboxEvent event = new OutboxEvent(); event.setId(UUID.randomUUID()); event.setAggregateType("PAYMENT"); event.setAggregateId(payment.getId()); event.setEventType(eventType); event.setEventVersion(1); event.setPayload(payload(payment)); event.setCorrelationId(payment.getCorrelationId()); event.setStatus("PENDING"); event.setAttemptCount(0); event.setAvailableAt(now); event.setCreatedAt(now); return event;
    }

    private String payload(Payment payment) { try { return objectMapper.writeValueAsString(java.util.Map.of("paymentId", payment.getId(), "orderId", payment.getOrderId(), "amountVnd", payment.getAmountVnd(), "timing", payment.getTiming(), "method", payment.getMethod(), "status", payment.getStatus())); } catch (JacksonException exception) { throw new IllegalStateException(exception); } }
    private Payment requirePayment(UUID id) { return payments.findById(id).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy khoản thanh toán.")); }
    private PaymentResponse response(Payment payment, String redirectUrl) { return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmountVnd(), payment.getTiming(), payment.getMethod(), payment.getStatus(), redirectUrl, payment.getExpiresAt(), payment.getPaidAt(), payment.getCodReceiptNo(), payment.getCodConfirmedBy(), payment.getCodConfirmedAt()); }
    private void validateCombination(OrderPaymentContextRequest request) { if (request.method() == OrderPaymentContextRequest.PaymentMethod.COD && request.timing() != OrderPaymentContextRequest.PaymentTiming.POSTPAID) throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "PREPAID_COD_FORBIDDEN", "COD chỉ hỗ trợ thanh toán trả sau."); }
}
