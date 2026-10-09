package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.dto.request.OrderPaymentContextRequest;
import com.dynamicmart.payment_service.dto.response.PaymentAttemptResponse;
import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.dto.response.PaymentResponse;
import com.dynamicmart.payment_service.outbox.OutboxEvent;
import com.dynamicmart.payment_service.entity.Payment;
import com.dynamicmart.payment_service.entity.PaymentAttempt;
import com.dynamicmart.payment_service.mapper.PaymentMapper;
import com.dynamicmart.payment_service.outbox.OutboxEventRepository;
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
import com.dynamicmart.payment_service.dto.response.PaymentPageResponse;

@Service
public class PaymentService {
    private static final Duration VNPAY_TTL = Duration.ofMinutes(15);
    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final OutboxEventRepository outbox;
    private final PaymentGatewayRouter gateways;
    private final ObjectMapper objectMapper;

    public enum CallbackOutcome { APPLIED, DUPLICATE, LATE_AUDIT }

    public PaymentService(PaymentRepository payments, PaymentAttemptRepository attempts, OutboxEventRepository outbox, PaymentGatewayRouter gateways, ObjectMapper objectMapper) {
        this.payments = payments; this.attempts = attempts; this.outbox = outbox; this.gateways = gateways; this.objectMapper = objectMapper;
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
        if (request.timing() == OrderPaymentContextRequest.PaymentTiming.PREPAID && request.method() != OrderPaymentContextRequest.PaymentMethod.COD && "PENDING".equals(payment.getStatus())) {
            PaymentAttempt attempt = reusableAttempt(payment);
            return response(payment, attempt.getRedirectUrl());
        }
        return response(payment, null);
    }

    @Transactional
    public PaymentResponse createVnPayAttempt(UUID paymentId) {
        Payment payment = requirePaymentForUpdate(paymentId);
        if ("COD".equals(payment.getMethod())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_METHOD_NOT_ONLINE", "Khoản thanh toán COD không tạo đường dẫn online.");
        if (!"PENDING".equals(payment.getStatus())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_PENDING", "Chỉ có thể tạo đường dẫn VNPay cho khoản đang chờ thanh toán.");
        if ("PREPAID".equals(payment.getTiming()) && payment.getExpiresAt().isBefore(Instant.now())) {
            expirePrepaid(payment); throw new PaymentException(HttpStatus.GONE, "PAYMENT_EXPIRED", "Khoản thanh toán trả trước đã hết hạn.");
        }
        PaymentAttempt latest = attempts.findByPaymentIdOrderByAttemptNoDesc(paymentId).stream().findFirst().orElse(null);
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus())) && latest.getExpiresAt() != null && latest.getExpiresAt().isAfter(Instant.now())) return response(payment, latest.getRedirectUrl());
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus()))) {
            latest.setStatus("EXPIRED"); latest.setUpdatedAt(Instant.now()); attempts.save(latest);
        }
        return response(payment, createOnlineAttempt(payment).getRedirectUrl());
    }

    @Transactional
    public PaymentResponse createVnPayAttemptForOrder(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_FOR_ORDER_NOT_FOUND", "Không tìm thấy Payment cho PaymentDue."));
        if (!"POSTPAID".equals(payment.getTiming()) || "COD".equals(payment.getMethod())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_DUE_NOT_APPLICABLE", "PaymentDue chỉ áp dụng cho phương thức online trả sau.");
        return createVnPayAttempt(payment.getId());
    }

    @Transactional
    public PaymentResponse createCustomerOnlineAttempt(UUID orderId, UUID customerId) {
        Payment payment = requireOwnedPaymentByOrder(orderId, customerId);
        if ("COD".equals(payment.getMethod())) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_METHOD_NOT_ONLINE", "Thanh toán khi nhận hàng không tạo đường dẫn thanh toán trực tuyến.");
        }
        if ("POSTPAID".equals(payment.getTiming())
                && attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).isEmpty()) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_DUE", "Đơn hàng chưa đến bước thanh toán trả sau.");
        }
        return createVnPayAttempt(payment.getId());
    }

    @Transactional
    public PaymentResponse collectCodForReceivedOrder(UUID orderId) {
        Payment payment = payments.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_FOR_ORDER_NOT_FOUND", "Không tìm thấy Payment của đơn hàng."));
        if (!"COD".equals(payment.getMethod()) || !"POSTPAID".equals(payment.getTiming())) throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_COD", "Chỉ có thể xác nhận thu tiền cho COD trả sau.");
        if ("PAID".equals(payment.getStatus())) return response(payment, null);
        if (!"PENDING".equals(payment.getStatus())) throw new PaymentException(HttpStatus.CONFLICT, "COD_STATUS_INVALID", "Trạng thái COD không cho phép ghi nhận đã thanh toán.");
        Instant now = Instant.now();
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID()); attempt.setPaymentId(payment.getId()); attempt.setAttemptNo(attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).size() + 1);
        attempt.setProvider("COD"); attempt.setProviderReference("CUSTOMER_RECEIVED_" + orderId); attempt.setAmountVnd(payment.getAmountVnd()); attempt.setStatus("SUCCEEDED"); attempt.setCreatedAt(now); attempt.setUpdatedAt(now);
        attempts.save(attempt);
        payment.setStatus("PAID"); payment.setPaidAt(now); payment.setCodConfirmedBy(payment.getCustomerId());
        payment.setCodConfirmedAt(now); payment.setUpdatedAt(now); payments.save(payment);
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
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_FOR_ORDER_NOT_FOUND", "Không tìm thấy Payment của đơn hàng."));
        return response(payment, activeRedirect(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getCustomerPaymentByOrder(UUID orderId, UUID customerId) {
        Payment payment = requireOwnedPaymentByOrder(orderId, customerId);
        return response(payment, activeRedirect(payment));
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
        return attempts.findByPaymentIdOrderByAttemptNoDesc(paymentId).stream()
                .map(PaymentMapper::toAttemptResponse).toList();
    }

    @Transactional
    public CallbackOutcome applyVnPaySuccess(String reference, String transactionReference, long amountVnd) {
        PaymentAttempt attempt = requireAttemptForUpdate(reference);
        Payment payment = requirePaymentForUpdate(attempt.getPaymentId());
        if (attempt.getAmountVnd() != amountVnd || payment.getAmountVnd() != amountVnd) return CallbackOutcome.DUPLICATE;
        if ("PAID".equals(payment.getStatus())) return CallbackOutcome.DUPLICATE;
        if (!"PENDING".equals(payment.getStatus())) return CallbackOutcome.LATE_AUDIT;
        if (transactionReference == null || transactionReference.isBlank()) return CallbackOutcome.DUPLICATE;
        if (payments.existsByProviderTransactionRefAndIdNot(transactionReference, payment.getId())) return CallbackOutcome.DUPLICATE;
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
        PaymentAttempt attempt = requireAttemptForUpdate(reference);
        Payment payment = requirePaymentForUpdate(attempt.getPaymentId());
        if (attempt.getAmountVnd() != amountVnd || payment.getAmountVnd() != amountVnd) return CallbackOutcome.DUPLICATE;
        if (!"PENDING".equals(payment.getStatus())) return "PREPAID".equals(payment.getTiming()) ? CallbackOutcome.LATE_AUDIT : CallbackOutcome.DUPLICATE;
        if ("FAILED".equals(attempt.getStatus()) || "EXPIRED".equals(attempt.getStatus())) return CallbackOutcome.DUPLICATE;
        Instant now = Instant.now(); attempt.setStatus("FAILED"); attempt.setUpdatedAt(now); attempts.save(attempt);
        if ("PREPAID".equals(payment.getTiming())) { payment.setStatus("FAILED"); payment.setFailedAt(now); payment.setUpdatedAt(now); payments.save(payment); publishFailed(payment); }
        return CallbackOutcome.APPLIED;
    }

    @Transactional
    public void expireDuePrepaidPayments() {
        payments.findDueForUpdate("PENDING", "PREPAID", Instant.now()).forEach(this::expirePrepaid);
    }

    private Payment newPayment(OrderPaymentContextRequest request) {
        Instant now = Instant.now(); Payment payment = new Payment();
        payment.setId(UUID.randomUUID()); payment.setOrderId(request.orderId()); payment.setCustomerId(request.customerId()); payment.setAmountVnd(request.amountVnd()); payment.setTiming(request.timing().name()); payment.setMethod(request.method().name()); payment.setStatus("PENDING"); payment.setCorrelationId(request.correlationId());
        if (request.timing() == OrderPaymentContextRequest.PaymentTiming.PREPAID && request.method() != OrderPaymentContextRequest.PaymentMethod.COD) payment.setExpiresAt(now.plus(VNPAY_TTL));
        payment.setCreatedAt(now); payment.setUpdatedAt(now); return payment;
    }

    private PaymentAttempt createOnlineAttempt(Payment payment) {
        Instant now = Instant.now(); int attemptNo = attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).size() + 1;
        Instant expires = now.plus(VNPAY_TTL);
        if (payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(expires)) expires = payment.getExpiresAt();
        PaymentGatewayRouter.CreatedPayment created = gateways.create(payment.getMethod(), payment.getId(), payment.getOrderId(), payment.getCustomerId(), payment.getAmountVnd(), attemptNo, expires);
        PaymentAttempt attempt = new PaymentAttempt(); attempt.setId(UUID.randomUUID()); attempt.setPaymentId(payment.getId()); attempt.setAttemptNo(attemptNo); attempt.setProvider(payment.getMethod()); attempt.setProviderReference(created.reference()); attempt.setAmountVnd(payment.getAmountVnd()); attempt.setStatus("CREATED");
        attempt.setExpiresAt(expires); attempt.setRedirectUrl(created.redirectUrl()); attempt.setCreatedAt(now); attempt.setUpdatedAt(now); attempts.save(attempt); return attempt;
    }

    /**
     * Idempotent Order retries may arrive after an earlier provider URL has expired. Reuse only a
     * still-active attempt; returning an expired URL would strand the customer on a dead payment page.
     */
    private PaymentAttempt reusableAttempt(Payment payment) {
        Instant now = Instant.now();
        PaymentAttempt latest = attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).stream().findFirst().orElse(null);
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus()))
                && latest.getExpiresAt() != null && latest.getExpiresAt().isAfter(now)) {
            return latest;
        }
        if (payment.getExpiresAt() != null && !payment.getExpiresAt().isAfter(now)) {
            expirePrepaid(payment);
            throw new PaymentException(HttpStatus.GONE, "PAYMENT_EXPIRED", "Khoản thanh toán trả trước đã hết hạn.");
        }
        if (latest != null && ("CREATED".equals(latest.getStatus()) || "REDIRECTED".equals(latest.getStatus()))) {
            latest.setStatus("EXPIRED"); latest.setUpdatedAt(now); attempts.save(latest);
        }
        return createOnlineAttempt(payment);
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

    private String payload(Payment payment) { try { return objectMapper.writeValueAsString(java.util.Map.of("paymentId", payment.getId(), "orderId", payment.getOrderId(), "customerId", payment.getCustomerId(), "amountVnd", payment.getAmountVnd(), "timing", payment.getTiming(), "method", payment.getMethod(), "paymentMethod", payment.getMethod(), "status", payment.getStatus())); } catch (JacksonException exception) { throw new IllegalStateException(exception); } }
    private Payment requirePayment(UUID id) { return payments.findById(id).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy khoản thanh toán.")); }
    private Payment requirePaymentForUpdate(UUID id) { return payments.findByIdForUpdate(id).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Không tìm thấy khoản thanh toán.")); }
    private Payment requireOwnedPaymentByOrder(UUID orderId, UUID customerId) {
        Payment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_FOR_ORDER_NOT_FOUND", "Không tìm thấy khoản thanh toán của đơn hàng."));
        if (customerId == null || !payment.getCustomerId().equals(customerId)) {
            throw new PaymentException(HttpStatus.FORBIDDEN, "PAYMENT_OWNERSHIP_DENIED", "Bạn không có quyền xem khoản thanh toán này.");
        }
        return payment;
    }
    private String activeRedirect(Payment payment) {
        if (!"PENDING".equals(payment.getStatus())) return null;
        Instant now = Instant.now();
        return attempts.findByPaymentIdOrderByAttemptNoDesc(payment.getId()).stream()
                .filter(attempt -> ("CREATED".equals(attempt.getStatus()) || "REDIRECTED".equals(attempt.getStatus()))
                        && attempt.getExpiresAt() != null && attempt.getExpiresAt().isAfter(now))
                .map(PaymentAttempt::getRedirectUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }
    private PaymentAttempt requireAttemptForUpdate(String reference) { return attempts.findByProviderReferenceForUpdate(reference).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_REFERENCE_NOT_FOUND", "Không tìm thấy mã thanh toán VNPay.")); }
    private PaymentResponse response(Payment payment, String redirectUrl) {
        return PaymentMapper.toResponse(payment, redirectUrl);
    }
    private void validateCombination(OrderPaymentContextRequest request) { if (request.method() == OrderPaymentContextRequest.PaymentMethod.COD && request.timing() != OrderPaymentContextRequest.PaymentTiming.POSTPAID) throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "PREPAID_COD_FORBIDDEN", "COD chỉ hỗ trợ thanh toán trả sau."); }
}
