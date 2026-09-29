package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.api.OrderException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CartCheckoutClient {
    private final RestClient client;
    private final String internalKey;
    public CartCheckoutClient(RestClient.Builder builder,
                              @Value("${app.clients.cart-service-url}") String baseUrl,
                              @Value("${app.security.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(baseUrl).build(); this.internalKey = internalKey;
    }

    public Preview preview(String token, PreviewRequest request) {
        try {
            Preview result = client.post().uri("/api/v1/cart/checkout/preview")
                    .header("Authorization", "Bearer " + token).body(request).retrieve().body(Preview.class);
            if (result == null) throw unavailable("CHECKOUT_PREVIEW_EMPTY", "Cart Service không trả về bản xem trước.");
            return result;
        } catch (RestClientException exception) {
            throw unavailable("CHECKOUT_REVALIDATION_FAILED", "Giỏ hàng, địa chỉ hoặc voucher đã thay đổi. Vui lòng kiểm tra lại.");
        }
    }

    public Reservation reserve(ReserveRequest request) {
        try {
            Reservation result = client.post().uri("/api/v1/cart/internal/voucher-reservations")
                    .header("X-Internal-Api-Key", internalKey).body(request).retrieve().body(Reservation.class);
            if (result == null) throw unavailable("VOUCHER_RESERVATION_EMPTY", "Không thể giữ lượt voucher.");
            return result;
        } catch (RestClientException exception) { throw unavailable("VOUCHER_RESERVATION_FAILED", "Voucher không còn khả dụng."); }
    }

    public void consume(UUID reservationId, UUID orderId) {
        client.post().uri("/api/v1/cart/internal/voucher-reservations/{id}/consume", reservationId)
                .header("X-Internal-Api-Key", internalKey).body(new ConsumeRequest(orderId)).retrieve().toBodilessEntity();
    }
    public void release(UUID reservationId, String reason) {
        try { client.post().uri("/api/v1/cart/internal/voucher-reservations/{id}/release", reservationId)
                .header("X-Internal-Api-Key", internalKey).body(new ReleaseRequest(reason)).retrieve().toBodilessEntity(); }
        catch (RestClientException ignored) { }
    }
    public void orderConfirmed(UUID eventId, UUID correlationId, UUID customerId, List<PurchasedItem> items) {
        try { client.post().uri("/api/v1/cart/internal/order-confirmed").header("X-Internal-Api-Key", internalKey)
                .body(new OrderConfirmedRequest(eventId, correlationId, customerId, "CART", items)).retrieve().toBodilessEntity(); }
        catch (RestClientException exception) { throw unavailable("CART_CLEANUP_FAILED", "Chưa thể chốt giỏ hàng; hệ thống sẽ thử lại."); }
    }

    private OrderException unavailable(String code, String message) { return new OrderException(HttpStatus.UNPROCESSABLE_ENTITY, code, message); }
    public record PreviewRequest(UUID addressId, UUID merchandiseVoucherId, String merchandiseCode,
                                 UUID shippingVoucherId, String shippingCode) { }
    public record Preview(UUID cartId, Address address, List<Item> items, long listSubtotalVnd,
                          long directSaleDiscountVnd, long itemsSubtotalVnd, Voucher merchandiseVoucher,
                          Voucher shippingVoucher, long merchandiseDiscountVnd, long shippingDiscountVnd,
                          ShippingQuote shippingQuote, long finalTotalVnd) { }
    public record Address(UUID id, String recipientName, String phone, String addressLine, int provinceId,
                          int wardId, String provinceName, String wardName) { }
    public record Item(UUID cartItemId, long cartItemVersion, UUID productId, UUID categoryId, UUID variantId,
                       String sku, String productName, String variantName, String imageUrl, UUID directSalePromotionId,
                       int quantity, long listPriceVnd, long directSaleDiscountVnd, long unitPriceVnd,
                       long lineTotalVnd, int weightGrams, int lengthCm, int widthCm, int heightCm) { }
    public record Voucher(UUID id, String code, String name, String scope, String discountMethod,
                          Long fixedDiscountVnd, Integer discountRateBps, Set<UUID> productIds,
                          Set<UUID> categoryIds, boolean eligible, String ineligibleReason, long discountAmountVnd) { }
    public record ShippingQuote(UUID quoteId, long feeVnd, long shippingDiscountVnd, long payableFeeVnd,
                                int serviceId, String serviceName, String eta, Instant expiresAt) { }
    public record ReserveRequest(UUID voucherId, UUID customerId, UUID checkoutSessionId, long orderSubtotalVnd,
                                 long eligibleSubtotalVnd, long shippingFeeVnd, Set<UUID> productIds,
                                 Set<UUID> categoryIds, Instant reservedUntil) { }
    public record Reservation(UUID id, UUID voucherId, UUID customerId, UUID checkoutSessionId, UUID orderId,
                              String status, long discountAmountVnd, long shippingDiscountVnd, Instant reservedUntil) { }
    private record ConsumeRequest(UUID orderId) { }
    private record ReleaseRequest(String reason) { }
    public record PurchasedItem(UUID id, int quantity, long version) { }
    private record OrderConfirmedRequest(UUID eventId, UUID correlationId, UUID customerId, String source,
                                         List<PurchasedItem> items) { }
}
