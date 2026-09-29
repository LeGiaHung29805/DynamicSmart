package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the durable admission point for Create Order before any remote reservation is attempted.
 * The Checkout row lock serializes requests for one session; remote calls belong to the later
 * orchestrator and must happen after this transaction commits.
 */
@Service
public class OrderCreationAdmissionService {
    private static final String OPERATION = "CREATE_ORDER";

    private final CheckoutSessionRepository sessions;
    private final CheckoutShippingQuoteRepository quotes;
    private final OrderSagaRepository sagas;
    private final Clock clock;

    public OrderCreationAdmissionService(
            CheckoutSessionRepository sessions,
            CheckoutShippingQuoteRepository quotes,
            OrderSagaRepository sagas,
            Clock clock) {
        this.sessions = sessions;
        this.quotes = quotes;
        this.sagas = sagas;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = OrderException.class)
    public AdmissionResult admit(UUID customerId, UUID checkoutSessionId, UUID idempotencyKey) {
        requireIdentifier(customerId, "customerId");
        requireIdentifier(checkoutSessionId, "checkoutSessionId");
        requireIdentifier(idempotencyKey, "Idempotency-Key");

        Instant now = Instant.now(clock);
        CheckoutSession session = sessions.findOwnedForUpdate(checkoutSessionId, customerId)
                .orElseThrow(() -> new OrderException(HttpStatus.NOT_FOUND, "CHECKOUT_SESSION_NOT_FOUND",
                        "Không tìm thấy Checkout Session: " + checkoutSessionId));
        String requestHash = requestHash(customerId, checkoutSessionId);

        OrderSaga sameKey = sagas.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (sameKey != null) {
            if (!sameKey.getRequestHash().equals(requestHash)
                    || !sameKey.getCheckoutSessionId().equals(checkoutSessionId)) {
                throw conflict("IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã được dùng cho một yêu cầu Create Order khác.");
            }
            return new AdmissionResult(sameKey.getId(), sameKey.getCorrelationId(), true);
        }

        OrderSaga sameSession = sagas.findByCheckoutSessionId(checkoutSessionId).orElse(null);
        if (sameSession != null) {
            throw conflict("ORDER_CREATION_ALREADY_STARTED",
                    "Checkout Session đã bắt đầu tạo Order bằng một Idempotency-Key khác.");
        }

        requireReadySession(session, now);
        var quote = quotes.findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        checkoutSessionId, ShippingQuoteStatus.ACTIVE, now)
                .orElseThrow(() -> conflict("ACTIVE_SHIPPING_QUOTE_REQUIRED",
                        "Checkout cần Preview lại để có báo giá giao hàng còn hiệu lực."));
        if (quote.getInputFingerprint() == null
                || !quote.getInputFingerprint().matches("[0-9a-fA-F]{64}")) {
            throw conflict("INVALID_SHIPPING_QUOTE_FINGERPRINT",
                    "Báo giá giao hàng không có fingerprint hợp lệ.");
        }

        OrderSaga saga = OrderSaga.start(
                UUID.randomUUID(), checkoutSessionId, idempotencyKey, requestHash, UUID.randomUUID(), now);
        sagas.save(saga);
        return new AdmissionResult(saga.getId(), saga.getCorrelationId(), false);
    }

    private void requireReadySession(CheckoutSession session, Instant now) {
        if (session.getStatus() == CheckoutStatus.ACTIVE && !session.getExpiresAt().isAfter(now)) {
            session.setStatus(CheckoutStatus.EXPIRED);
            session.setUpdatedAt(now);
            throw conflict("CHECKOUT_SESSION_EXPIRED", "Checkout Session đã hết hạn.");
        }
        if (session.getStatus() != CheckoutStatus.ACTIVE) {
            throw conflict("CHECKOUT_SESSION_NOT_ACTIVE", "Checkout Session không còn ở trạng thái ACTIVE.");
        }
        if (session.getCompletedOrderId() != null) {
            throw conflict("CHECKOUT_SESSION_ALREADY_COMPLETED", "Checkout Session đã tạo Order.");
        }
        if (session.getAddressId() == null) {
            throw invalid("CHECKOUT_ADDRESS_REQUIRED", "Cần chọn và Preview địa chỉ trước khi tạo Order.");
        }
        if (session.getPaymentTiming() == null || session.getPaymentMethod() == null) {
            throw invalid("PAYMENT_SELECTION_REQUIRED", "Cần chọn phương thức thanh toán trước khi tạo Order.");
        }
        requireValidPaymentPair(session.getPaymentTiming(), session.getPaymentMethod(), session.getFinalTotalVnd());
        if (session.getItemsListSubtotalVnd() == null || session.getDirectSaleDiscountVnd() == null
                || session.getItemsSubtotalVnd() == null || session.getProductDiscountVnd() == null
                || session.getOrderDiscountVnd() == null || session.getShippingFeeVnd() == null
                || session.getShippingDiscountVnd() == null || session.getFinalTotalVnd() == null) {
            throw invalid("CHECKOUT_PREVIEW_REQUIRED", "Checkout cần Preview thành công trước khi tạo Order.");
        }
    }

    private void requireValidPaymentPair(PaymentTiming timing, PaymentMethod method, Long finalTotal) {
        if (finalTotal == null) {
            return;
        }
        boolean free = finalTotal == 0 && timing == PaymentTiming.NOT_REQUIRED && method == PaymentMethod.FREE;
        boolean paid = finalTotal > 0 && ((timing == PaymentTiming.PREPAID && method == PaymentMethod.VNPAY)
                || (timing == PaymentTiming.POSTPAID
                && (method == PaymentMethod.VNPAY || method == PaymentMethod.COD)));
        if (!free && !paid) {
            throw invalid("INVALID_PAYMENT_SELECTION",
                    "Phương thức thanh toán không phù hợp với tổng tiền Checkout.");
        }
    }

    private String requestHash(UUID customerId, UUID checkoutSessionId) {
        String canonical = OPERATION + "|" + customerId + "|" + checkoutSessionId;
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không có SHA-256.", exception);
        }
    }

    private void requireIdentifier(UUID value, String field) {
        if (value == null) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "MISSING_IDENTIFIER", field + " không được để trống.");
        }
    }

    private OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    private OrderException invalid(String code, String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    public record AdmissionResult(UUID sagaId, UUID correlationId, boolean replay) {
    }
}
