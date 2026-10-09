package com.dynamicmart.order_service.standalone;

import com.dynamicmart.order_service.client.AddressGateway;
import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway;
import com.dynamicmart.order_service.client.CheckoutSelectionGateway.TrustedCheckoutItem;
import com.dynamicmart.order_service.client.InventoryReservationGateway;
import com.dynamicmart.order_service.client.PaymentClient;
import com.dynamicmart.order_service.client.VoucherPricingGateway;
import com.dynamicmart.order_service.client.VoucherReservationGateway;
import com.dynamicmart.order_service.exception.OrderException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

/**
 * In-process substitutes for services owned by other teams.
 *
 * <p>This configuration is deliberately available only with the {@code standalone} profile. It lets
 * Postman exercise the real Order database, transactions, pricing, saga, idempotency and lifecycle
 * while Identity, Cart, Catalog and Payment are stopped.</p>
 */
@Configuration
@Profile("standalone")
public class StandaloneGatewayConfiguration {
    public static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    public static final UUID OTHER_CUSTOMER_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");
    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final UUID ADDRESS_ID = UUID.fromString("01000000-0000-4000-8000-000000000001");
    public static final UUID OTHER_ADDRESS_ID = UUID.fromString("01000000-0000-4000-8000-000000000002");
    public static final UUID CART_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    public static final UUID OTHER_CART_ID = UUID.fromString("70000000-0000-4000-8000-000000000002");
    public static final UUID PHONE_PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID PHONE_VARIANT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    public static final UUID LAPTOP_PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    public static final UUID LAPTOP_VARIANT_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
    public static final UUID MERCHANDISE_VOUCHER_ID = UUID.fromString("71000000-0000-4000-8000-000000000001");
    public static final UUID SHIPPING_VOUCHER_ID = UUID.fromString("71000000-0000-4000-8000-000000000002");

    @Bean
    AddressGateway standaloneAddressGateway() {
        return (customerId, addressId) -> {
            if (CUSTOMER_ID.equals(customerId) && ADDRESS_ID.equals(addressId)) {
                return new AddressGateway.AddressSnapshot(
                        ADDRESS_ID, "Khách hàng Demo", "0900000002",
                        "41A Đường Phú Diễn", 201, 11007, "Hà Nội", "Phường Phú Diễn");
            }
            if (OTHER_CUSTOMER_ID.equals(customerId) && OTHER_ADDRESS_ID.equals(addressId)) {
                return new AddressGateway.AddressSnapshot(
                        OTHER_ADDRESS_ID, "Khách hàng Thứ Hai", "0900000003",
                        "12 Đường Cầu Giấy", 201, 11006, "Hà Nội", "Phường Cầu Giấy");
            }
            throw invalid("CHECKOUT_ADDRESS_INVALID", "Địa chỉ mock không tồn tại hoặc không thuộc khách hàng.");
        };
    }

    @Bean
    CheckoutSelectionGateway standaloneCheckoutSelectionGateway() {
        return new CheckoutSelectionGateway() {
            @Override
            public TrustedCheckoutSelection loadSelectedCartItems(UUID customerId, UUID cartId) {
                UUID expectedCart = CUSTOMER_ID.equals(customerId) ? CART_ID
                        : OTHER_CUSTOMER_ID.equals(customerId) ? OTHER_CART_ID : null;
                if (!cartId.equals(expectedCart)) {
                    throw invalid("CHECKOUT_CART_NOT_FOUND", "Giỏ hàng mock không tồn tại hoặc không thuộc khách hàng.");
                }
                return new TrustedCheckoutSelection(cartId, List.of(phoneCartItem(), laptopCartItem()));
            }

            @Override
            public TrustedCheckoutSelection loadBuyNowItem(UUID customerId, UUID variantId, int quantity) {
                requireCustomer(customerId);
                if (quantity < 1 || quantity > 20) {
                    throw invalid("CHECKOUT_VARIANT_UNAVAILABLE", "Số lượng mock phải từ 1 đến 20.");
                }
                TrustedCheckoutItem item;
                if (PHONE_VARIANT_ID.equals(variantId)) {
                    item = phoneItem(null, null, quantity);
                } else if (LAPTOP_VARIANT_ID.equals(variantId)) {
                    item = laptopItem(null, null, quantity);
                } else {
                    throw invalid("CHECKOUT_VARIANT_UNAVAILABLE", "Variant không có trong Catalog mock.");
                }
                return new TrustedCheckoutSelection(null, List.of(item));
            }
        };
    }

    @Bean
    StandaloneVoucherGateway standaloneVoucherGateway() {
        return new StandaloneVoucherGateway();
    }

    @Bean
    StandaloneInventoryGateway standaloneInventoryGateway() {
        return new StandaloneInventoryGateway();
    }

    @Bean
    PaymentClient standalonePaymentClient(Clock clock) {
        return new StandalonePaymentClient(clock);
    }

    @Bean
    CartOrderConfirmationGateway standaloneCartOrderConfirmationGateway() {
        Map<UUID, CartOrderConfirmationGateway.OrderConfirmedCommand> handledEvents = new ConcurrentHashMap<>();
        return command -> handledEvents.putIfAbsent(command.eventId(), command);
    }

    private static TrustedCheckoutItem phoneCartItem() {
        return phoneItem(UUID.fromString("72000000-0000-4000-8000-000000000001"), 1L, 1);
    }

    private static TrustedCheckoutItem laptopCartItem() {
        return laptopItem(UUID.fromString("72000000-0000-4000-8000-000000000002"), 3L, 1);
    }

    private static TrustedCheckoutItem phoneItem(UUID cartItemId, Long version, int quantity) {
        return new TrustedCheckoutItem(
                cartItemId, version, PHONE_PRODUCT_ID, PHONE_VARIANT_ID, "DPP-BLK-128",
                "Dynamic Phone Pro", "Đen / 128 GB",
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9",
                15_990_000, UUID.fromString("70200000-0000-4000-8000-000000000001"),
                500_000, quantity, 420, 18, 10, 6);
    }

    private static TrustedCheckoutItem laptopItem(UUID cartItemId, Long version, int quantity) {
        return new TrustedCheckoutItem(
                cartItemId, version, LAPTOP_PRODUCT_ID, LAPTOP_VARIANT_ID, "DLA-BLK-512",
                "Dynamic Laptop Air", "Đen / 512 GB",
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853",
                24_990_000, null, 0, quantity, 1800, 42, 30, 8);
    }

    private static void requireCustomer(UUID customerId) {
        if (!CUSTOMER_ID.equals(customerId) && !OTHER_CUSTOMER_ID.equals(customerId)) {
            throw invalid("STANDALONE_CUSTOMER_UNKNOWN", "Customer không có trong bộ dữ liệu standalone.");
        }
    }

    private static OrderException invalid(String code, String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }

    static final class StandaloneVoucherGateway implements VoucherPricingGateway, VoucherReservationGateway {
        private final Map<UUID, UUID> reservationsByOperation = new ConcurrentHashMap<>();
        private final Map<UUID, ReservationState> reservations = new ConcurrentHashMap<>();

        @Override
        public VoucherPreview preview(VoucherPreviewRequest request) {
            requireCustomer(request.customerId());
            if (request.items().isEmpty()) {
                throw invalid("STANDALONE_CART_EMPTY", "Không có dòng hàng để áp voucher.");
            }
            List<AppliedVoucher> applied = new ArrayList<>();
            List<LineDiscount> lines = new ArrayList<>();
            long subtotal = request.items().stream()
                    .mapToLong(item -> Math.multiplyExact(item.unitPriceVnd(), item.quantity()))
                    .sum();

            if (request.merchandiseVoucherId() != null) {
                if (!MERCHANDISE_VOUCHER_ID.equals(request.merchandiseVoucherId())) {
                    throw invalid("VOUCHER_NOT_FOUND", "Voucher hàng hóa không có trong dữ liệu mock.");
                }
                long discount = Math.min(subtotal / 10, 1_000_000);
                long allocated = 0;
                for (int index = 0; index < request.items().size(); index++) {
                    VoucherItem item = request.items().get(index);
                    long share = index == request.items().size() - 1
                            ? discount - allocated
                            : discount * Math.multiplyExact(item.unitPriceVnd(), item.quantity()) / subtotal;
                    allocated += share;
                    lines.add(new LineDiscount(item.variantId(), 0, share));
                }
                applied.add(new AppliedVoucher(
                        MERCHANDISE_VOUCHER_ID, "WELCOME10", "ORDER_DISCOUNT", "PERCENTAGE",
                        10L, subtotal, discount, 0));
            }
            if (request.shippingVoucherId() != null) {
                if (!SHIPPING_VOUCHER_ID.equals(request.shippingVoucherId())) {
                    throw invalid("VOUCHER_NOT_FOUND", "Voucher giao hàng không có trong dữ liệu mock.");
                }
                long shippingDiscount = Math.min(request.shippingFeeVnd(), 50_000);
                applied.add(new AppliedVoucher(
                        SHIPPING_VOUCHER_ID, "FREESHIP50", "SHIPPING_DISCOUNT", "FIXED_AMOUNT",
                        50_000L, request.shippingFeeVnd(), 0, shippingDiscount));
            }
            return new VoucherPreview(applied, lines);
        }

        @Override
        public Reservation reserve(ReserveVoucherRequest request) {
            UUID id = reservationsByOperation.computeIfAbsent(
                    request.operationKey(), ignored -> UUID.nameUUIDFromBytes(
                            ("standalone-voucher|" + request.sagaId()).getBytes(StandardCharsets.UTF_8)));
            reservations.putIfAbsent(id, ReservationState.RESERVED);
            return new Reservation(id);
        }

        @Override
        public void consume(ConsumeVoucherRequest request) {
            transition(request.reservationId(), ReservationState.CONSUMED);
        }

        @Override
        public void release(ReleaseVoucherRequest request) {
            transition(request.reservationId(), ReservationState.RELEASED);
        }

        private void transition(UUID id, ReservationState target) {
            ReservationState current = reservations.get(id);
            if (current == null) {
                throw invalid("VOUCHER_RESERVATION_NOT_FOUND", "Không tìm thấy voucher reservation mock.");
            }
            if (current != ReservationState.RESERVED && current != target) {
                throw invalid("VOUCHER_RESERVATION_STATE_INVALID", "Voucher reservation mock đã kết thúc ở trạng thái khác.");
            }
            reservations.put(id, target);
        }
    }

    static final class StandaloneInventoryGateway implements InventoryReservationGateway {
        private final Map<UUID, UUID> reservationsByOperation = new ConcurrentHashMap<>();
        private final Map<UUID, ReservationState> reservations = new ConcurrentHashMap<>();

        @Override
        public Reservation reserve(ReserveInventoryRequest request) {
            if (request.lines().isEmpty() || request.lines().stream().anyMatch(line -> line.quantity() < 1 || line.quantity() > 20)) {
                throw invalid("INSUFFICIENT_STOCK", "Catalog mock chỉ cho phép mỗi dòng từ 1 đến 20 sản phẩm.");
            }
            if (request.lines().stream().anyMatch(line -> !PHONE_VARIANT_ID.equals(line.variantId())
                    && !LAPTOP_VARIANT_ID.equals(line.variantId()))) {
                throw invalid("VARIANT_NOT_FOUND", "Variant không có trong Inventory mock.");
            }
            UUID id = reservationsByOperation.computeIfAbsent(
                    request.operationKey(), ignored -> UUID.nameUUIDFromBytes(
                            ("standalone-inventory|" + request.sagaId()).getBytes(StandardCharsets.UTF_8)));
            reservations.putIfAbsent(id, ReservationState.RESERVED);
            return new Reservation(id);
        }

        @Override
        public void commit(CommitInventoryRequest request) {
            transition(request.reservationId(), ReservationState.COMMITTED);
        }

        @Override
        public void release(ReleaseInventoryRequest request) {
            transition(request.reservationId(), ReservationState.RELEASED);
        }

        private void transition(UUID id, ReservationState target) {
            ReservationState current = reservations.get(id);
            if (current == null) {
                throw invalid("INVENTORY_RESERVATION_NOT_FOUND", "Không tìm thấy inventory reservation mock.");
            }
            if (current != ReservationState.RESERVED && current != target) {
                throw invalid("INVENTORY_RESERVATION_STATE_INVALID", "Inventory reservation mock đã kết thúc ở trạng thái khác.");
            }
            reservations.put(id, target);
        }
    }

    static final class StandalonePaymentClient implements PaymentClient {
        private final Clock clock;
        private final Map<UUID, ShippingQuoteResponse> quotes = new ConcurrentHashMap<>();
        private final Map<UUID, PaymentResponse> paymentsByOrder = new ConcurrentHashMap<>();
        private final Map<UUID, PaymentResponse> paymentsById = new ConcurrentHashMap<>();

        StandalonePaymentClient(Clock clock) {
            this.clock = clock;
        }

        @Override
        public ShippingQuoteResponse createShippingQuote(ShippingQuoteRequest request) {
            requireCustomer(request.customerId());
            if (request.items() == null || request.items().isEmpty()) {
                throw invalid("SHIPPING_ITEMS_REQUIRED", "Báo giá mock cần ít nhất một sản phẩm.");
            }
            long fee = request.items().stream().mapToLong(item -> (long) item.weightGrams() * item.quantity()).sum() > 2_000
                    ? 40_000 : 30_000;
            if (request.shippingDiscountVnd() < 0 || request.shippingDiscountVnd() > fee) {
                throw invalid("SHIPPING_DISCOUNT_INVALID", "Giảm phí vận chuyển mock không hợp lệ.");
            }
            String fingerprint = sha256(request.toString());
            // A quote is a consumable resource. Equal quote inputs may have the same fingerprint,
            // but must not reuse an ID across different Checkout Sessions.
            UUID quoteId = UUID.randomUUID();
            Instant now = Instant.now(clock);
            ShippingQuoteResponse quote = new ShippingQuoteResponse(
                    quoteId, fee, request.shippingDiscountVnd(), fee - request.shippingDiscountVnd(),
                    53320, "GHN Mock - Đường bộ", "Giao dự kiến 2-3 ngày",
                    now.plus(15, ChronoUnit.MINUTES), fingerprint);
            quotes.put(quoteId, quote);
            return quote;
        }

        @Override
        public ShippingQuoteResponse validateAndConsumeQuote(UUID quoteId, QuoteValidationRequest request) {
            ShippingQuoteResponse quote = quotes.get(quoteId);
            if (quote == null || !quote.requestFingerprint().equals(request.requestFingerprint())
                    || !quote.expiresAt().isAfter(Instant.now(clock))) {
                throw new OrderException(HttpStatus.CONFLICT, "SHIPPING_QUOTE_INVALID",
                        "Báo giá mock không tồn tại, hết hạn hoặc không khớp Checkout.");
            }
            requireCustomer(request.customerId());
            return quote;
        }

        @Override
        public PaymentResponse createPayment(OrderPaymentContextRequest request) {
            PaymentResponse response = paymentsByOrder.computeIfAbsent(request.orderId(), orderId -> {
                UUID id = UUID.nameUUIDFromBytes(("standalone-payment|" + orderId).getBytes(StandardCharsets.UTF_8));
                Instant expiresAt = Instant.now(clock).plus(30, ChronoUnit.MINUTES);
                return new PaymentResponse(
                        id, orderId, request.amountVnd(), request.timing().name(), request.method().name(),
                        "PENDING", request.method().isOnline() ? "http://localhost:8084/api/v1/standalone/payment-success" : null,
                        expiresAt, null);
            });
            paymentsById.putIfAbsent(response.id(), response);
            return response;
        }

        @Override
        public PaymentResponse getPayment(UUID paymentId) {
            PaymentResponse payment = paymentsById.get(paymentId);
            if (payment == null) {
                throw invalid("PAYMENT_NOT_FOUND", "Không tìm thấy Payment mock.");
            }
            return payment;
        }

        @Override
        public PaymentResponse createVnPayAttempt(UUID paymentId) {
            return getPayment(paymentId);
        }

        @Override
        public PaymentResponse collectCodForReceivedOrder(UUID orderId) {
            PaymentResponse current = paymentsByOrder.get(orderId);
            if (current == null) {
                throw invalid("PAYMENT_NOT_FOUND", "Không tìm thấy Payment COD mock.");
            }
            PaymentResponse paid = new PaymentResponse(
                    current.id(), current.orderId(), current.amountVnd(), current.timing(), current.method(),
                    "SUCCEEDED", current.redirectUrl(), current.expiresAt(), Instant.now(clock));
            paymentsByOrder.put(orderId, paid);
            paymentsById.put(paid.id(), paid);
            return paid;
        }
    }

    private enum ReservationState {
        RESERVED,
        COMMITTED,
        CONSUMED,
        RELEASED
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
