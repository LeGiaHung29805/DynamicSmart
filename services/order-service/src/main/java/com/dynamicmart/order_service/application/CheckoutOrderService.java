package com.dynamicmart.order_service.application;

import static com.dynamicmart.order_service.api.CheckoutDtos.*;

import com.dynamicmart.order_service.api.OrderException;
import com.dynamicmart.order_service.client.CartCheckoutClient;
import com.dynamicmart.order_service.client.CartCheckoutClient.Preview;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSessionItem;
import com.dynamicmart.order_service.entity.CheckoutSessionVoucher;
import com.dynamicmart.order_service.entity.CustomerOrder;
import com.dynamicmart.order_service.entity.OrderAddress;
import com.dynamicmart.order_service.entity.OrderItem;
import com.dynamicmart.order_service.entity.OrderShippingSnapshot;
import com.dynamicmart.order_service.entity.OrderVoucherSnapshot;
import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CustomerOrderRepository;
import com.dynamicmart.order_service.repository.OrderAddressRepository;
import com.dynamicmart.order_service.repository.OrderItemRepository;
import com.dynamicmart.order_service.repository.OrderShippingSnapshotRepository;
import com.dynamicmart.order_service.repository.OrderVoucherSnapshotRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutOrderService {
    private final CartCheckoutClient cart; private final PaymentClient payments;
    private final CheckoutSessionRepository sessions; private final CheckoutSessionItemRepository sessionItems;
    private final CheckoutSessionVoucherRepository sessionVouchers; private final CustomerOrderRepository orders;
    private final OrderAddressRepository addresses; private final OrderItemRepository orderItems;
    private final OrderVoucherSnapshotRepository voucherSnapshots; private final OrderShippingSnapshotRepository shippingSnapshots;
    private final OrderFinalizationQueue finalization;

    public CheckoutOrderService(CartCheckoutClient cart, PaymentClient payments, CheckoutSessionRepository sessions,
                                CheckoutSessionItemRepository sessionItems, CheckoutSessionVoucherRepository sessionVouchers,
                                CustomerOrderRepository orders, OrderAddressRepository addresses, OrderItemRepository orderItems,
                                OrderVoucherSnapshotRepository voucherSnapshots, OrderShippingSnapshotRepository shippingSnapshots,
                                OrderFinalizationQueue finalization) {
        this.cart = cart; this.payments = payments; this.sessions = sessions; this.sessionItems = sessionItems;
        this.sessionVouchers = sessionVouchers; this.orders = orders; this.addresses = addresses;
        this.orderItems = orderItems; this.voucherSnapshots = voucherSnapshots; this.shippingSnapshots = shippingSnapshots;
        this.finalization = finalization;
    }

    @Transactional
    public OrderResult create(UUID customerId, String jwtToken, UUID idempotencyKey, CreateOrderRequest request) {
        CustomerOrder existing = orders.findById(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getCustomerId().equals(customerId)) throw invalid(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", "Idempotency-Key đã được sử dụng.");
            CheckoutSession previous = sessions.findById(existing.getCheckoutSessionId()).orElseThrow(() ->
                    invalid(HttpStatus.CONFLICT, "CHECKOUT_SESSION_MISSING", "Không tìm thấy phiên Checkout của đơn đã tạo."));
            boolean paymentChanged = existing.getFinalTotalVnd() > 0 && (!existing.getPaymentTiming().equals(request.paymentTiming())
                    || !existing.getPaymentMethod().equals(request.paymentMethod()));
            if (!request.addressId().equals(previous.getAddressId()) || paymentChanged)
                throw invalid(HttpStatus.CONFLICT, "IDEMPOTENCY_PAYLOAD_CONFLICT", "Idempotency-Key đã được dùng với nội dung Checkout khác.");
            PaymentClient.PaymentResponse payment = existing.getFinalTotalVnd() == 0 ? null :
                    payments.create(existing.getId(), customerId, existing.getFinalTotalVnd(), existing.getPaymentTiming(), existing.getPaymentMethod(), idempotencyKey);
            return result(existing, payment);
        }
        validatePayment(request);
        Preview preview = cart.preview(jwtToken, new CartCheckoutClient.PreviewRequest(request.addressId(),
                request.merchandiseVoucherId(), null, request.shippingVoucherId(), null));
        Instant now = Instant.now();
        if (!request.quoteId().equals(preview.shippingQuote().quoteId()) || !preview.shippingQuote().expiresAt().isAfter(now.plusSeconds(10)))
            throw invalid(HttpStatus.CONFLICT, "SHIPPING_QUOTE_STALE", "Báo giá giao hàng đã thay đổi hoặc hết hạn. Vui lòng tải lại Checkout.");
        if (preview.merchandiseVoucher() != null && !preview.merchandiseVoucher().eligible()) throw voucherInvalid(preview.merchandiseVoucher().ineligibleReason());
        if (preview.shippingVoucher() != null && !preview.shippingVoucher().eligible()) throw voucherInvalid(preview.shippingVoucher().ineligibleReason());

        UUID sessionId = UUID.nameUUIDFromBytes(("checkout:" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
        String fingerprint = fingerprint(customerId, request, preview);
        List<CartCheckoutClient.Reservation> reservations = new ArrayList<>();
        try {
            reserve(preview.merchandiseVoucher(), customerId, sessionId, preview, reservations);
            reserve(preview.shippingVoucher(), customerId, sessionId, preview, reservations);
            String timing = preview.finalTotalVnd() == 0 ? "NOT_REQUIRED" : request.paymentTiming();
            String method = preview.finalTotalVnd() == 0 ? "FREE" : request.paymentMethod();
            boolean confirmed = preview.finalTotalVnd() == 0 || "POSTPAID".equals(timing);
            CheckoutSession session = saveSession(sessionId, customerId, request, preview, fingerprint, timing, method, confirmed, now);
            saveSessionItems(sessionId, preview, now);
            saveSessionVouchers(sessionId, preview, reservations, now);
            CustomerOrder order = saveOrder(idempotencyKey, sessionId, customerId, preview, timing, method, confirmed, now);
            saveOrderDetails(order, preview, now);
            session.setCompletedOrderId(order.getId()); sessions.save(session);
            PaymentClient.PaymentResponse payment = preview.finalTotalVnd() == 0 ? null :
                    payments.create(order.getId(), customerId, preview.finalTotalVnd(), timing, method, idempotencyKey);
            if (confirmed) queueFinalization(order, preview, reservations, idempotencyKey);
            return result(order, payment);
        } catch (RuntimeException exception) {
            reservations.forEach(value -> cart.release(value.id(), "ORDER_CREATION_FAILED"));
            throw exception;
        }
    }

    private void validatePayment(CreateOrderRequest request) {
        boolean valid = ("PREPAID".equals(request.paymentTiming()) && "VNPAY".equals(request.paymentMethod()))
                || ("POSTPAID".equals(request.paymentTiming()) && ("VNPAY".equals(request.paymentMethod()) || "COD".equals(request.paymentMethod())));
        if (!valid) throw invalid(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_COMBINATION_INVALID", "Phương thức và thời điểm thanh toán không hợp lệ.");
    }

    private void reserve(CartCheckoutClient.Voucher voucher, UUID customerId, UUID sessionId, Preview preview,
                         List<CartCheckoutClient.Reservation> reservations) {
        if (voucher == null) return;
        long eligible = eligibleSubtotal(voucher, preview);
        var request = new CartCheckoutClient.ReserveRequest(voucher.id(), customerId, sessionId,
                preview.itemsSubtotalVnd(), eligible, preview.shippingQuote().feeVnd(),
                preview.items().stream().map(CartCheckoutClient.Item::productId).collect(Collectors.toSet()),
                preview.items().stream().map(CartCheckoutClient.Item::categoryId).collect(Collectors.toSet()),
                preview.shippingQuote().expiresAt());
        CartCheckoutClient.Reservation reservation = cart.reserve(request);
        long actual = "SHIPPING_DISCOUNT".equals(voucher.scope()) ? reservation.shippingDiscountVnd() : reservation.discountAmountVnd();
        if (actual != voucher.discountAmountVnd()) {
            cart.release(reservation.id(), "CHECKOUT_DISCOUNT_CHANGED");
            throw invalid(HttpStatus.CONFLICT, "VOUCHER_DISCOUNT_CHANGED", "Giá trị giảm của voucher đã thay đổi. Vui lòng tải lại Checkout.");
        }
        reservations.add(reservation);
    }

    private CheckoutSession saveSession(UUID id, UUID customerId, CreateOrderRequest request, Preview preview,
                                        String fingerprint, String timing, String method, boolean confirmed, Instant now) {
        CheckoutSession value = new CheckoutSession(); value.setId(id); value.setCustomerId(customerId); value.setSource("CART");
        value.setCartId(preview.cartId()); value.setSelectionFingerprint(fingerprint); value.setAddressId(request.addressId());
        value.setStatus("COMPLETED"); value.setPaymentTiming(timing); value.setPaymentMethod(method);
        value.setItemsListSubtotalVnd(preview.listSubtotalVnd()); value.setDirectSaleDiscountVnd(preview.directSaleDiscountVnd());
        value.setItemsSubtotalVnd(preview.itemsSubtotalVnd()); value.setProductDiscountVnd(productDiscount(preview));
        value.setOrderDiscountVnd(orderDiscount(preview)); value.setShippingFeeVnd(preview.shippingQuote().feeVnd());
        value.setShippingDiscountVnd(preview.shippingDiscountVnd()); value.setFinalTotalVnd(preview.finalTotalVnd());
        value.setExpiresAt(preview.shippingQuote().expiresAt()); value.setCreatedAt(now); value.setUpdatedAt(now);
        return sessions.save(value);
    }

    private void saveSessionItems(UUID sessionId, Preview preview, Instant now) {
        sessionItems.saveAll(preview.items().stream().map(item -> {
            CheckoutSessionItem value = new CheckoutSessionItem(); value.setId(UUID.randomUUID()); value.setCheckoutSessionId(sessionId);
            value.setSourceCartItemId(item.cartItemId()); value.setSourceCartItemVersion(item.cartItemVersion());
            value.setProductId(item.productId()); value.setVariantId(item.variantId()); value.setSku(item.sku());
            value.setProductName(item.productName()); value.setVariantName(item.variantName()); value.setImageUrl(item.imageUrl());
            value.setListPriceVnd(item.listPriceVnd()); value.setDirectSalePromotionId(item.directSalePromotionId());
            value.setDirectSaleDiscountVnd(item.directSaleDiscountVnd()); value.setUnitPriceVnd(item.unitPriceVnd());
            value.setQuantity(item.quantity()); value.setWeightGrams(item.weightGrams()); value.setCreatedAt(now); return value;
        }).toList());
    }

    private void saveSessionVouchers(UUID sessionId, Preview preview, List<CartCheckoutClient.Reservation> reservations, Instant now) {
        for (CartCheckoutClient.Voucher voucher : Stream.of(preview.merchandiseVoucher(), preview.shippingVoucher()).filter(java.util.Objects::nonNull).toList()) {
            CartCheckoutClient.Reservation reservation = reservations.stream().filter(value -> value.voucherId().equals(voucher.id())).findFirst().orElseThrow();
            CheckoutSessionVoucher value = new CheckoutSessionVoucher(); value.setId(UUID.randomUUID()); value.setCheckoutSessionId(sessionId);
            value.setVoucherId(voucher.id()); value.setVoucherReservationId(reservation.id()); value.setVoucherCode(voucher.code());
            value.setScope(voucher.scope()); value.setDiscountAmountVnd(reservation.discountAmountVnd());
            value.setShippingDiscountVnd(reservation.shippingDiscountVnd()); value.setCreatedAt(now); value.setUpdatedAt(now);
            sessionVouchers.save(value);
        }
    }

    private CustomerOrder saveOrder(UUID id, UUID sessionId, UUID customerId, Preview preview, String timing,
                                    String method, boolean confirmed, Instant now) {
        CustomerOrder value = new CustomerOrder(); value.setId(id); value.setOrderNumber(orderNumber(id, now));
        value.setCheckoutSessionId(sessionId); value.setCustomerId(customerId); value.setStatus(confirmed ? "CONFIRMED" : "PENDING_PAYMENT");
        value.setPaymentTiming(timing); value.setPaymentMethod(method); value.setItemsListSubtotalVnd(preview.listSubtotalVnd());
        value.setDirectSaleDiscountVnd(preview.directSaleDiscountVnd()); value.setItemsSubtotalVnd(preview.itemsSubtotalVnd());
        value.setProductDiscountVnd(productDiscount(preview)); value.setOrderDiscountVnd(orderDiscount(preview));
        value.setShippingFeeVnd(preview.shippingQuote().feeVnd()); value.setShippingDiscountVnd(preview.shippingDiscountVnd());
        value.setFinalTotalVnd(preview.finalTotalVnd()); value.setCurrency("VND");
        value.setPaymentDueAt("PREPAID".equals(timing) ? preview.shippingQuote().expiresAt() : null);
        value.setConfirmedAt(confirmed ? now : null); value.setCreatedAt(now); value.setUpdatedAt(now); return orders.save(value);
    }

    private void saveOrderDetails(CustomerOrder order, Preview preview, Instant now) {
        OrderAddress address = new OrderAddress(); address.setId(UUID.randomUUID()); address.setOrderId(order.getId());
        address.setSourceAddressId(preview.address().id()); address.setRecipientName(preview.address().recipientName());
        address.setPhone(preview.address().phone()); address.setAddressLine(preview.address().addressLine());
        address.setProvinceId(preview.address().provinceId()); address.setWardId(preview.address().wardId());
        address.setProvinceName(preview.address().provinceName()); address.setWardName(preview.address().wardName());
        address.setCreatedAt(now); addresses.save(address);
        long remainingDiscount = preview.merchandiseDiscountVnd();
        for (CartCheckoutClient.Item item : preview.items()) {
            long allocated = Math.min(remainingDiscount, item.lineTotalVnd()); remainingDiscount -= allocated;
            OrderItem value = new OrderItem(); value.setId(UUID.randomUUID()); value.setOrderId(order.getId()); value.setProductId(item.productId());
            value.setVariantId(item.variantId()); value.setSku(item.sku()); value.setProductName(item.productName());
            value.setVariantName(item.variantName()); value.setImageUrl(item.imageUrl()); value.setListPriceVnd(item.listPriceVnd());
            value.setDirectSalePromotionId(item.directSalePromotionId()); value.setDirectSaleDiscountVnd(item.directSaleDiscountVnd());
            value.setUnitPriceVnd(item.unitPriceVnd()); value.setQuantity(item.quantity()); value.setProductDiscountVnd(0);
            value.setOrderDiscountVnd(allocated); value.setLineTotalVnd(item.lineTotalVnd() - allocated);
            value.setWeightGrams(item.weightGrams()); value.setCreatedAt(now); orderItems.save(value);
        }
        saveVoucherSnapshot(order.getId(), preview.merchandiseVoucher(), preview, now);
        saveVoucherSnapshot(order.getId(), preview.shippingVoucher(), preview, now);
        OrderShippingSnapshot shipping = new OrderShippingSnapshot(); shipping.setId(UUID.randomUUID()); shipping.setOrderId(order.getId());
        shipping.setQuoteId(preview.shippingQuote().quoteId()); shipping.setInputFingerprint(fingerprint(order.getCustomerId(), null, preview));
        shipping.setServiceId(preview.shippingQuote().serviceId()); shipping.setServiceName(preview.shippingQuote().serviceName());
        shipping.setFeeVnd(preview.shippingQuote().feeVnd()); shipping.setEta(parseInstant(preview.shippingQuote().eta()));
        shipping.setCreatedAt(now); shippingSnapshots.save(shipping);
    }

    private void saveVoucherSnapshot(UUID orderId, CartCheckoutClient.Voucher voucher, Preview preview, Instant now) {
        if (voucher == null) return;
        OrderVoucherSnapshot value = new OrderVoucherSnapshot(); value.setId(UUID.randomUUID()); value.setOrderId(orderId);
        value.setVoucherId(voucher.id()); value.setVoucherCode(voucher.code()); value.setScope(voucher.scope());
        value.setDiscountMethod(voucher.discountMethod()); value.setDiscountValue("FIXED_AMOUNT".equals(voucher.discountMethod()) ? voucher.fixedDiscountVnd() : voucher.discountRateBps() == null ? null : voucher.discountRateBps().longValue());
        value.setEligibleSubtotalVnd(eligibleSubtotal(voucher, preview));
        value.setDiscountAmountVnd("SHIPPING_DISCOUNT".equals(voucher.scope()) ? 0 : voucher.discountAmountVnd());
        value.setShippingDiscountVnd("SHIPPING_DISCOUNT".equals(voucher.scope()) ? voucher.discountAmountVnd() : 0);
        value.setCreatedAt(now); voucherSnapshots.save(value);
    }

    private void queueFinalization(CustomerOrder order, Preview preview, List<CartCheckoutClient.Reservation> reservations, UUID correlationId) {
        List<CartCheckoutClient.PurchasedItem> purchased = preview.items().stream()
                .map(value -> new CartCheckoutClient.PurchasedItem(value.cartItemId(), value.quantity(), value.cartItemVersion())).toList();
        finalization.enqueue(UUID.randomUUID(), order.getId(), correlationId, order.getCustomerId(),
                reservations.stream().map(CartCheckoutClient.Reservation::id).toList(), purchased);
    }

    private long eligibleSubtotal(CartCheckoutClient.Voucher voucher, Preview preview) {
        if ("SHIPPING_DISCOUNT".equals(voucher.scope())) return preview.itemsSubtotalVnd();
        boolean unrestricted = (voucher.productIds() == null || voucher.productIds().isEmpty()) && (voucher.categoryIds() == null || voucher.categoryIds().isEmpty());
        return preview.items().stream().filter(item -> unrestricted
                || (voucher.productIds() != null && voucher.productIds().contains(item.productId()))
                || (voucher.categoryIds() != null && voucher.categoryIds().contains(item.categoryId())))
                .mapToLong(CartCheckoutClient.Item::lineTotalVnd).sum();
    }
    private long productDiscount(Preview preview) { return preview.merchandiseVoucher() != null && "PRODUCT_DISCOUNT".equals(preview.merchandiseVoucher().scope()) ? preview.merchandiseDiscountVnd() : 0; }
    private long orderDiscount(Preview preview) { return preview.merchandiseDiscountVnd() - productDiscount(preview); }
    private OrderResult result(CustomerOrder order, PaymentClient.PaymentResponse payment) { return new OrderResult(order.getId(), order.getOrderNumber(), order.getStatus(), payment == null ? null : payment.id(), payment == null ? null : payment.redirectUrl()); }
    private String orderNumber(UUID id, Instant now) { return "DM" + now.toString().substring(0, 10).replace("-", "") + id.toString().replace("-", "").substring(0, 10).toUpperCase(); }
    private String fingerprint(UUID customerId, CreateOrderRequest request, Preview preview) {
        String input = customerId + "|" + (request == null ? "snapshot" : request.toString()) + "|" + preview.cartId() + "|" + preview.items() + "|" + preview.finalTotalVnd() + "|" + preview.shippingQuote().quoteId();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private Instant parseInstant(String value) { try { return value == null ? null : Instant.parse(value); } catch (DateTimeParseException ignored) { return null; } }
    private OrderException voucherInvalid(String reason) { return invalid(HttpStatus.UNPROCESSABLE_ENTITY, "VOUCHER_INELIGIBLE", reason == null ? "Voucher không hợp lệ." : reason); }
    private OrderException invalid(HttpStatus status, String code, String message) { return new OrderException(status, code, message); }
}
