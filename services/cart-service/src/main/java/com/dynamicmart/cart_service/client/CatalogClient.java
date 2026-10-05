package com.dynamicmart.cart_service.client;

import com.dynamicmart.cart_service.exception.CartException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CatalogClient {
    private final RestClient client;
    private final String internalKey;

    public CatalogClient(RestClient.Builder builder,
                         @Value("${app.clients.catalog-service-url:http://localhost:8082}") String baseUrl,
                         @Value("${app.security.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(baseUrl).build();
        this.internalKey = internalKey;
    }

    public List<VariantSnapshot> validate(List<VariantQuantity> items) {
        if (items.isEmpty()) return List.of();
        try {
            CatalogEnvelope response = client.post().uri("/api/v1/catalog/internal/variants/validate")
                    .header("X-Internal-Api-Key", internalKey)
                    .body(new ValidateRequest(items))
                    .retrieve().body(CatalogEnvelope.class);
            if (response == null || response.data() == null) throw unavailable();
            return response.data();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    public VariantSnapshot requirePurchasable(UUID productId, UUID variantId, int quantity) {
        VariantSnapshot snapshot = validate(List.of(new VariantQuantity(variantId, quantity))).stream()
                .filter(value -> variantId.equals(value.variantId())).findFirst()
                .orElseThrow(() -> new CartException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "CART_VARIANT_NOT_FOUND", "Variant không tồn tại."));
        if (!productId.equals(snapshot.productId())) throw new CartException(HttpStatus.UNPROCESSABLE_ENTITY,
                "CART_PRODUCT_VARIANT_MISMATCH", "Variant không thuộc Product đã chọn.");
        if (!snapshot.purchasable() || snapshot.availableQuantity() < quantity) {
            String message = snapshot.unavailableReason() == null || snapshot.unavailableReason().isBlank()
                    ? "Variant đã ngừng bán hoặc không đủ tồn kho." : snapshot.unavailableReason();
            throw new CartException(HttpStatus.UNPROCESSABLE_ENTITY, "CART_VARIANT_UNAVAILABLE", message);
        }
        return snapshot;
    }

    public void requirePublicProduct(UUID productId) {
        try {
            ProductEnvelope response = client.get().uri("/api/v1/catalog/products/id/{productId}", productId)
                    .retrieve().body(ProductEnvelope.class);
            if (response == null || response.data() == null || !productId.equals(response.data().id())) throw unavailable();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new CartException(HttpStatus.UNPROCESSABLE_ENTITY, "WISHLIST_PRODUCT_NOT_AVAILABLE",
                        "Sản phẩm không tồn tại hoặc đã ngừng bán.");
            }
            throw unavailable();
        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private CartException unavailable() {
        return new CartException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                "Không thể kiểm tra Variant và tồn kho lúc này.");
    }

    public record VariantQuantity(UUID variantId, int quantity) { }
    private record ValidateRequest(List<VariantQuantity> items) { }
    private record CatalogEnvelope(List<VariantSnapshot> data) { }
    private record ProductEnvelope(ProductRef data) { }
    private record ProductRef(UUID id) { }
    public record VariantSnapshot(UUID productId, UUID categoryId, UUID variantId, String productName, String variantName,
                                  String sku, PriceSnapshot price, int availableQuantity, int weightGrams,
                                  Integer lengthCm, Integer widthCm, Integer heightCm, String imageUrl,
                                  boolean purchasable, String unavailableReason) { }
    public record PriceSnapshot(long listPriceVnd, Long salePriceVnd, long directSaleDiscountVnd,
                                Integer directSalePercent, java.time.Instant directSaleEndsAt,
                                UUID directSalePromotionId) {
        public long effectivePriceVnd() { return salePriceVnd == null ? listPriceVnd : salePriceVnd; }
    }
}
