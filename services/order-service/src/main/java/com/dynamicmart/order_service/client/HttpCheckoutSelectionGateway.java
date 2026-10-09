package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.exception.OrderException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!standalone")
public class HttpCheckoutSelectionGateway implements CheckoutSelectionGateway {
    private final RestClient cart; private final RestClient catalog;
    public HttpCheckoutSelectionGateway(@Qualifier("cartRestClient") RestClient cart,
                                        @Qualifier("catalogRestClient") RestClient catalog) { this.cart = cart; this.catalog = catalog; }
    @Override public TrustedCheckoutSelection loadSelectedCartItems(UUID customerId, UUID cartId) {
        try {
            CartSelection selection = cart.get().uri("/api/v1/cart/internal/checkout-selections/{customerId}/{cartId}", customerId, cartId)
                    .retrieve().body(CartSelection.class);
            if (selection == null || !cartId.equals(selection.cartId())) {
                throw contractInvalid("Cart trả về cartId không khớp request.");
            }
            if (selection.items() == null || selection.items().isEmpty()) {
                throw invalid("CHECKOUT_CART_EMPTY", "Giỏ hàng không còn dòng được chọn.");
            }
            Map<UUID, CartLine> source = cartLines(selection.items());
            List<CatalogVariant> variants = validate(selection.items().stream().map(value -> new VariantQuantity(value.variantId(), value.quantity())).toList());
            return new TrustedCheckoutSelection(selection.cartId(), variants.stream()
                    .map(value -> trusted(source.get(value.variantId()), value)).toList());
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw dependencyUnavailable("CHECKOUT_SELECTION_UNAVAILABLE", "Không thể lấy giỏ hàng đã chọn.");
        }
    }
    @Override public TrustedCheckoutSelection loadBuyNowItem(UUID customerId, UUID variantId, int quantity) {
        CatalogVariant value = validate(List.of(new VariantQuantity(variantId, quantity))).stream().findFirst()
                .orElseThrow(() -> invalid("CHECKOUT_VARIANT_UNAVAILABLE", "Variant không tồn tại."));
        return new TrustedCheckoutSelection(null, List.of(trusted(new CartLine(null, null, value.productId(), variantId, quantity), value)));
    }
    private List<CatalogVariant> validate(List<VariantQuantity> items) {
        try {
            CatalogEnvelope result = catalog.post().uri("/api/v1/catalog/internal/variants/validate")
                    .body(new ValidateRequest(items)).retrieve().body(CatalogEnvelope.class);
            if (result == null || result.data() == null) {
                throw contractInvalid("Catalog không trả danh sách Variant.");
            }
            Map<UUID, CatalogVariant> variants = new LinkedHashMap<>();
            for (CatalogVariant value : result.data()) {
                if (value == null || value.variantId() == null || variants.putIfAbsent(value.variantId(), value) != null) {
                    throw contractInvalid("Catalog trả Variant thiếu ID hoặc bị trùng.");
                }
            }
            if (variants.size() != items.size()
                    || items.stream().map(VariantQuantity::variantId).anyMatch(id -> !variants.containsKey(id))) {
                throw contractInvalid("Catalog không trả đúng tập Variant được yêu cầu.");
            }
            return items.stream().map(item -> variants.get(item.variantId())).toList();
        } catch (OrderException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw dependencyUnavailable("CATALOG_UNAVAILABLE", "Không thể kiểm tra Catalog.");
        }
    }
    private TrustedCheckoutItem trusted(CartLine source, CatalogVariant value) {
        if (source == null || !Objects.equals(source.productId(), value.productId()) || !value.purchasable()
                || value.availableQuantity() < source.quantity() || value.price() == null) {
            throw invalid("CHECKOUT_VARIANT_UNAVAILABLE",
                    value.unavailableReason() == null ? "Variant không còn mua được." : value.unavailableReason());
        }
        if (value.weightGrams() < 1 || value.lengthCm() == null || value.lengthCm() < 1
                || value.widthCm() == null || value.widthCm() < 1
                || value.heightCm() == null || value.heightCm() < 1) {
            throw contractInvalid("Catalog phải trả trọng lượng và kích thước Variant hợp lệ.");
        }
        return new TrustedCheckoutItem(source.cartItemId(), source.cartItemVersion(), value.productId(), value.variantId(),
                value.sku(), value.productName(), value.variantName(), value.imageUrl(), value.price().listPriceVnd(),
                value.price().directSalePromotionId(), value.price().directSaleDiscountVnd(), source.quantity(),
                value.weightGrams(), value.lengthCm(), value.widthCm(), value.heightCm());
    }
    private Map<UUID, CartLine> cartLines(List<CartLine> items) {
        Map<UUID, CartLine> values = new LinkedHashMap<>();
        for (CartLine item : items) {
            if (item == null || item.cartItemId() == null || item.cartItemVersion() == null
                    || item.cartItemVersion() < 0 || item.productId() == null || item.variantId() == null
                    || item.quantity() < 1 || values.putIfAbsent(item.variantId(), item) != null) {
                throw contractInvalid("Cart trả dòng Checkout thiếu dữ liệu hoặc trùng Variant.");
            }
        }
        return values;
    }
    private OrderException invalid(String code, String message) {
        return new OrderException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
    private OrderException dependencyUnavailable(String code, String message) {
        return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
    private OrderException contractInvalid(String message) {
        return new OrderException(HttpStatus.BAD_GATEWAY, "CHECKOUT_SOURCE_CONTRACT_INVALID", message);
    }
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
