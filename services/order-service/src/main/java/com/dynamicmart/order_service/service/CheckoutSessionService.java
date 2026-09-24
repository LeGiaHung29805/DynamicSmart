package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutItem;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutSelection;
import com.dynamicmart.order_service.config.CheckoutProperties;
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.UpdateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.response.CheckoutSessionItemResponse;
import com.dynamicmart.order_service.dto.response.CheckoutSessionResponse;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.CheckoutStatus;
import com.dynamicmart.order_service.entity.PaymentMethod;
import com.dynamicmart.order_service.entity.PaymentTiming;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import com.dynamicmart.order_service.exception.OrderException;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutSessionService {
    private final CheckoutSessionRepository sessions;
    private final CheckoutSessionItemRepository sessionItems;
    private final CheckoutSessionVoucherRepository sessionVouchers;
    private final CheckoutShippingQuoteRepository shippingQuotes;
    private final OrderSagaRepository orderSagas;
    private final CheckoutSelectionGateway selectionGateway;
    private final CheckoutProperties properties;
    private final Clock clock;

    public CheckoutSessionService(
            CheckoutSessionRepository sessions,
            CheckoutSessionItemRepository sessionItems,
            CheckoutSessionVoucherRepository sessionVouchers,
            CheckoutShippingQuoteRepository shippingQuotes,
            OrderSagaRepository orderSagas,
            CheckoutSelectionGateway selectionGateway,
            CheckoutProperties properties,
            Clock clock) {
        this.sessions = sessions;
        this.sessionItems = sessionItems;
        this.sessionVouchers = sessionVouchers;
        this.shippingQuotes = shippingQuotes;
        this.orderSagas = orderSagas;
        this.selectionGateway = selectionGateway;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CheckoutSessionResponse create(UUID customerId, CreateCheckoutSessionRequest request) {
        validateCreateRequest(request);
        TrustedCheckoutSelection selection = loadTrustedSelection(customerId, request);
        validateTrustedSelection(request, selection);

        Instant now = Instant.now(clock);
        UUID sessionId = UUID.randomUUID();
        CheckoutSession session = CheckoutSession.create(
                sessionId,
                customerId,
                request.source(),
                request.cartId(),
                fingerprint(selection.items()),
                now.plus(properties.sessionTtl()),
                now);
        sessions.save(session);

        List<CheckoutSessionItem> items = selection.items().stream()
                .map(item -> toEntity(sessionId, item, now))
                .toList();
        sessionItems.saveAll(items);
        return toResponse(session, items);
    }

    @Transactional
    public CheckoutSessionResponse get(UUID customerId, UUID sessionId) {
        CheckoutSession session = findOwned(customerId, sessionId);
        expireIfNeeded(session, Instant.now(clock));
        return toResponse(session, sessionItems.findAllByCheckoutSessionId(sessionId));
    }

    @Transactional(noRollbackFor = OrderException.class)
    public CheckoutSessionResponse update(UUID customerId, UUID sessionId, UpdateCheckoutSessionRequest request) {
        if (request.addressId() == null && request.paymentTiming() == null && request.paymentMethod() == null) {
            throw invalidSource("Cần gửi ít nhất một trường có thể cập nhật.");
        }
        validatePaymentSelection(request);
        CheckoutSession session = sessions.findOwnedForUpdate(sessionId, customerId)
                .orElseThrow(() -> notFound(sessionId));
        requireActive(session, Instant.now(clock));
        requireCreationNotStarted(sessionId);
        if (request.addressId() != null) {
            if (!request.addressId().equals(session.getAddressId())) {
                invalidatePreview(session, Instant.now(clock));
            }
            session.setAddressId(request.addressId());
        }
        if (request.paymentTiming() != null) {
            session.setPaymentTiming(request.paymentTiming());
        }
        if (request.paymentMethod() != null) {
            session.setPaymentMethod(request.paymentMethod());
        }
        session.setUpdatedAt(Instant.now(clock));
        return toResponse(session, sessionItems.findAllByCheckoutSessionId(sessionId));
    }

    @Transactional(noRollbackFor = OrderException.class)
    public CheckoutSessionResponse cancel(UUID customerId, UUID sessionId) {
        CheckoutSession session = sessions.findOwnedForUpdate(sessionId, customerId)
                .orElseThrow(() -> notFound(sessionId));
        requireCreationNotStarted(sessionId);
        requireActiveOrAlreadyCancelled(session, Instant.now(clock));
        if (session.getStatus() == CheckoutStatus.ACTIVE) {
            session.setStatus(CheckoutStatus.CANCELLED);
            session.setUpdatedAt(Instant.now(clock));
        }
        return toResponse(session, sessionItems.findAllByCheckoutSessionId(sessionId));
    }

    private TrustedCheckoutSelection loadTrustedSelection(UUID customerId, CreateCheckoutSessionRequest request) {
        return request.source() == CheckoutSource.CART
                ? selectionGateway.loadSelectedCartItems(customerId, request.cartId())
                : selectionGateway.loadBuyNowItem(customerId, request.variantId(), request.quantity());
    }

    private void validateCreateRequest(CreateCheckoutSessionRequest request) {
        if (request.source() == CheckoutSource.CART) {
            if (request.cartId() == null || request.variantId() != null || request.quantity() != null) {
                throw invalidSource("Nguồn CART chỉ nhận cartId; không nhận variantId hoặc quantity từ client.");
            }
            return;
        }
        if (request.source() == CheckoutSource.BUY_NOW) {
            if (request.cartId() != null || request.variantId() == null || request.quantity() == null || request.quantity() <= 0) {
                throw invalidSource("Nguồn BUY_NOW yêu cầu variantId và quantity, không nhận cartId.");
            }
            return;
        }
        throw invalidSource("source không được hỗ trợ.");
    }

    private void validateTrustedSelection(CreateCheckoutSessionRequest request, TrustedCheckoutSelection selection) {
        if (selection == null || selection.items() == null || selection.items().isEmpty()) {
            throw invalidSelection("Cart/Catalog không trả về sản phẩm có thể checkout.");
        }
        if (request.source() == CheckoutSource.CART && !request.cartId().equals(selection.cartId())) {
            throw invalidSelection("Cart trả về cartId không khớp với lựa chọn của khách hàng.");
        }
        if (request.source() == CheckoutSource.BUY_NOW && (selection.cartId() != null || selection.items().size() != 1)) {
            throw invalidSelection("BUY_NOW chỉ được chứa đúng một dòng sản phẩm và không gắn cart.");
        }
        for (TrustedCheckoutItem item : selection.items()) {
            validateTrustedItem(request, item);
        }
    }

    private void validateTrustedItem(CreateCheckoutSessionRequest request, TrustedCheckoutItem item) {
        if (item == null || item.productId() == null || item.variantId() == null
                || isBlank(item.sku()) || isBlank(item.productName())
                || item.listPriceVnd() < 0 || item.directSaleDiscountVnd() < 0
                || item.directSaleDiscountVnd() > item.listPriceVnd()
                || item.quantity() <= 0 || item.weightGrams() <= 0
                || item.lengthCm() <= 0 || item.widthCm() <= 0 || item.heightCm() <= 0) {
            throw invalidSelection("Snapshot sản phẩm từ Cart/Catalog không hợp lệ.");
        }
        if (request.source() == CheckoutSource.CART
                && (item.sourceCartItemId() == null || item.sourceCartItemVersion() == null || item.sourceCartItemVersion() < 0)) {
            throw invalidSelection("Mỗi dòng CART phải có id và version nguồn.");
        }
        if (request.source() == CheckoutSource.BUY_NOW
                && (item.sourceCartItemId() != null || item.sourceCartItemVersion() != null
                || !request.variantId().equals(item.variantId()) || request.quantity() != item.quantity())) {
            throw invalidSelection("Snapshot BUY_NOW không khớp variantId hoặc quantity đã chọn.");
        }
    }

    private void validatePaymentSelection(UpdateCheckoutSessionRequest request) {
        if (request.paymentTiming() == null && request.paymentMethod() == null) {
            return;
        }
        if (request.paymentTiming() == null || request.paymentMethod() == null) {
            throw invalidPaymentSelection("paymentTiming và paymentMethod phải được chọn cùng nhau.");
        }
        if (request.paymentTiming() == PaymentTiming.NOT_REQUIRED || request.paymentMethod() == PaymentMethod.FREE) {
            throw invalidPaymentSelection("Chỉ backend được gán NOT_REQUIRED + FREE cho đơn có tổng tiền bằng 0.");
        }
        boolean validPair = (request.paymentTiming() == PaymentTiming.PREPAID && request.paymentMethod() == PaymentMethod.VNPAY)
                || (request.paymentTiming() == PaymentTiming.POSTPAID
                && (request.paymentMethod() == PaymentMethod.VNPAY || request.paymentMethod() == PaymentMethod.COD));
        if (!validPair) {
            throw invalidPaymentSelection("Cặp paymentTiming/paymentMethod không hợp lệ.");
        }
    }

    private CheckoutSession findOwned(UUID customerId, UUID sessionId) {
        return sessions.findByIdAndCustomerId(sessionId, customerId).orElseThrow(() -> notFound(sessionId));
    }

    private void requireActive(CheckoutSession session, Instant now) {
        expireIfNeeded(session, now);
        if (session.getStatus() == CheckoutStatus.EXPIRED) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_EXPIRED", "Checkout Session đã hết hạn.");
        }
        if (session.getStatus() != CheckoutStatus.ACTIVE) {
            throw new OrderException(HttpStatus.CONFLICT, "CHECKOUT_SESSION_NOT_ACTIVE",
                    "Checkout Session không còn ở trạng thái ACTIVE.");
        }
    }

    private void requireActiveOrAlreadyCancelled(CheckoutSession session, Instant now) {
        expireIfNeeded(session, now);
        if (session.getStatus() == CheckoutStatus.CANCELLED) {
            return;
        }
        requireActive(session, now);
    }

    private void expireIfNeeded(CheckoutSession session, Instant now) {
        if (session.getStatus() == CheckoutStatus.ACTIVE && !session.getExpiresAt().isAfter(now)) {
            session.setStatus(CheckoutStatus.EXPIRED);
            session.setUpdatedAt(now);
        }
    }

    private void invalidatePreview(CheckoutSession session, Instant now) {
        shippingQuotes.findAllByCheckoutSessionIdAndStatus(session.getId(), ShippingQuoteStatus.ACTIVE)
                .forEach(quote -> quote.invalidate(now));
        sessionVouchers.deleteAllByCheckoutSessionId(session.getId());
        session.setItemsListSubtotalVnd(null);
        session.setDirectSaleDiscountVnd(null);
        session.setItemsSubtotalVnd(null);
        session.setProductDiscountVnd(null);
        session.setOrderDiscountVnd(null);
        session.setShippingFeeVnd(null);
        session.setShippingDiscountVnd(null);
        session.setFinalTotalVnd(null);
        if (session.getPaymentTiming() == PaymentTiming.NOT_REQUIRED || session.getPaymentMethod() == PaymentMethod.FREE) {
            session.setPaymentTiming(null);
            session.setPaymentMethod(null);
        }
    }

    private void requireCreationNotStarted(UUID sessionId) {
        if (orderSagas.existsByCheckoutSessionId(sessionId)) {
            throw new OrderException(HttpStatus.CONFLICT, "ORDER_CREATION_IN_PROGRESS",
                    "Checkout Session đang được dùng để tạo Order và không thể thay đổi hoặc hủy.");
        }
    }

    private CheckoutSessionItem toEntity(UUID sessionId, TrustedCheckoutItem item, Instant now) {
        return CheckoutSessionItem.create(
                UUID.randomUUID(), sessionId, item.sourceCartItemId(), item.sourceCartItemVersion(),
                item.productId(), item.variantId(), item.sku(), item.productName(), item.variantName(), item.imageUrl(),
                item.listPriceVnd(), item.directSalePromotionId(), item.directSaleDiscountVnd(),
                item.quantity(), item.weightGrams(), item.lengthCm(), item.widthCm(), item.heightCm(), now);
    }

    private CheckoutSessionResponse toResponse(CheckoutSession session, List<CheckoutSessionItem> items) {
        List<CheckoutSessionItemResponse> itemResponses = items.stream().map(item -> new CheckoutSessionItemResponse(
                item.getId(), item.getProductId(), item.getVariantId(), item.getSku(), item.getProductName(),
                item.getVariantName(), item.getImageUrl(), item.getListPriceVnd(), item.getDirectSaleDiscountVnd(),
                item.getUnitPriceVnd(), item.getQuantity(), item.getWeightGrams(),
                item.getLengthCm(), item.getWidthCm(), item.getHeightCm())).toList();
        return new CheckoutSessionResponse(
                session.getId(), session.getSource(), session.getCartId(), session.getAddressId(), session.getStatus(),
                session.getPaymentTiming(), session.getPaymentMethod(), session.getExpiresAt(), session.getCreatedAt(),
                session.getUpdatedAt(), itemResponses);
    }

    private String fingerprint(List<TrustedCheckoutItem> items) {
        String canonical = items.stream().sorted(Comparator.comparing(item -> item.variantId().toString()))
                .map(item -> String.join("|", item.productId().toString(), item.variantId().toString(), item.sku(),
                        Long.toString(item.listPriceVnd()), Long.toString(item.directSaleDiscountVnd()),
                        Integer.toString(item.quantity()), Integer.toString(item.weightGrams()),
                        Integer.toString(item.lengthCm()), Integer.toString(item.widthCm()), Integer.toString(item.heightCm()),
                        item.sourceCartItemId() == null ? "" : item.sourceCartItemId().toString(),
                        item.sourceCartItemVersion() == null ? "" : item.sourceCartItemVersion().toString()))
                .reduce((left, right) -> left + "\n" + right).orElseThrow();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không có SHA-256.", exception);
        }
    }

    private OrderException notFound(UUID sessionId) {
        return new OrderException(HttpStatus.NOT_FOUND, "CHECKOUT_SESSION_NOT_FOUND",
                "Không tìm thấy Checkout Session: " + sessionId);
    }

    private OrderException invalidSource(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_CHECKOUT_SOURCE", message);
    }

    private OrderException invalidSelection(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_TRUSTED_SELECTION", message);
    }

    private OrderException invalidPaymentSelection(String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PAYMENT_SELECTION", message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
