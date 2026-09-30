package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpCheckoutSelectionGateway implements CheckoutSelectionGateway {
    private final RestClient cart; private final RestClient catalog;
    public HttpCheckoutSelectionGateway(@Qualifier("cartRestClient") RestClient cart,
                                        @Qualifier("catalogRestClient") RestClient catalog) { this.cart = cart; this.catalog = catalog; }
    @Override public TrustedCheckoutSelection loadSelectedCartItems(UUID customerId, UUID cartId) {
        try {
            CartSelection selection = cart.get().uri("/api/v1/cart/internal/checkout-selections/{customerId}/{cartId}", customerId, cartId)
                    .retrieve().body(CartSelection.class);
            if (selection == null || selection.items() == null || selection.items().isEmpty()) throw unavailable("CHECKOUT_CART_EMPTY", "Giỏ hàng không còn dòng được chọn.");
            Map<UUID, CartLine> source = selection.items().stream().collect(Collectors.toMap(CartLine::variantId, value -> value));
            List<CatalogVariant> variants = validate(selection.items().stream().map(value -> new VariantQuantity(value.variantId(), value.quantity())).toList());
            return new TrustedCheckoutSelection(selection.cartId(), variants.stream().map(value -> trusted(source.get(value.variantId()), value)).toList());
        } catch (RestClientException exception) { throw unavailable("CHECKOUT_SELECTION_UNAVAILABLE", "Không thể lấy giỏ hàng đã chọn."); }
    }
    @Override public TrustedCheckoutSelection loadBuyNowItem(UUID customerId, UUID variantId, int quantity) {
        CatalogVariant value = validate(List.of(new VariantQuantity(variantId, quantity))).stream().findFirst()
                .orElseThrow(() -> unavailable("CHECKOUT_VARIANT_UNAVAILABLE", "Variant không tồn tại."));
        return new TrustedCheckoutSelection(null, List.of(trusted(new CartLine(null, null, value.productId(), variantId, quantity), value)));
    }
    private List<CatalogVariant> validate(List<VariantQuantity> items) {
        try {
            CatalogEnvelope result = catalog.post().uri("/api/v1/catalog/internal/variants/validate")
                    .body(new ValidateRequest(items)).retrieve().body(CatalogEnvelope.class);
            if (result == null || result.data() == null || result.data().size() != items.size()) throw unavailable("CATALOG_CONTRACT_INVALID", "Catalog không trả đủ Variant.");
            return result.data();
        } catch (RestClientException exception) { throw unavailable("CATALOG_UNAVAILABLE", "Không thể kiểm tra Catalog."); }
    }
    private TrustedCheckoutItem trusted(CartLine source, CatalogVariant value) {
        if (source == null || !source.productId().equals(value.productId()) || !value.purchasable()
                || value.availableQuantity() < source.quantity() || value.price() == null)
            throw unavailable("CHECKOUT_VARIANT_UNAVAILABLE", value.unavailableReason() == null ? "Variant không còn mua được." : value.unavailableReason());
        return new TrustedCheckoutItem(source.cartItemId(), source.cartItemVersion(), value.productId(), value.variantId(),
                value.sku(), value.productName(), value.variantName(), value.imageUrl(), value.price().listPriceVnd(),
                value.price().directSalePromotionId(), value.price().directSaleDiscountVnd(), source.quantity(),
                Math.max(1, value.weightGrams()), positive(value.lengthCm()), positive(value.widthCm()), positive(value.heightCm()));
    }
    private int positive(Integer value) { return value == null || value < 1 ? 1 : value; }
    private OrderException unavailable(String code, String message) { return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, code, message); }
    private record CartSelection(UUID cartId, List<CartLine> items) { }
    private record CartLine(UUID cartItemId, Long cartItemVersion, UUID productId, UUID variantId, int quantity) { }
    private record VariantQuantity(UUID variantId, int quantity) { }
    private record ValidateRequest(List<VariantQuantity> items) { }
    private record CatalogEnvelope(List<CatalogVariant> data) { }
    private record CatalogVariant(UUID productId, UUID categoryId, UUID variantId, String productName, String variantName,
                                  String sku, Price price, int availableQuantity, int weightGrams, Integer lengthCm,
                                  Integer widthCm, Integer heightCm, String imageUrl, boolean purchasable,
                                  String unavailableReason) { }
    private record Price(long listPriceVnd, Long salePriceVnd, long directSaleDiscountVnd,
                         Integer directSalePercent, java.time.Instant directSaleEndsAt, UUID directSalePromotionId) { }
}
