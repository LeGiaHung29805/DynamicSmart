package com.dynamicmart.order_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checkout_session_items")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutSessionItem {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "source_cart_item_id") private UUID sourceCartItemId;
    @Column(name = "source_cart_item_version") private Long sourceCartItemVersion;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(nullable = false, length = 100) private String sku;
    @Column(name = "product_name", nullable = false, length = 300) private String productName;
    @Column(name = "variant_name", length = 300) private String variantName;
    @Column(name = "image_url", length = 1000) private String imageUrl;
    @Column(name = "list_price_vnd", nullable = false) private long listPriceVnd;
    @Column(name = "direct_sale_promotion_id") private UUID directSalePromotionId;
    @Column(name = "direct_sale_discount_vnd", nullable = false) private long directSaleDiscountVnd;
    @Column(name = "unit_price_vnd", nullable = false) private long unitPriceVnd;
    @Column(nullable = false) private int quantity;
    @Column(name = "weight_grams", nullable = false) private int weightGrams;
    @Column(name = "length_cm") private Integer lengthCm;
    @Column(name = "width_cm") private Integer widthCm;
    @Column(name = "height_cm") private Integer heightCm;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static CheckoutSessionItem create(
            UUID id,
            UUID checkoutSessionId,
            UUID sourceCartItemId,
            Long sourceCartItemVersion,
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String variantName,
            String imageUrl,
            long listPriceVnd,
            UUID directSalePromotionId,
            long directSaleDiscountVnd,
            int quantity,
            int weightGrams,
            int lengthCm,
            int widthCm,
            int heightCm,
            Instant now) {
        CheckoutSessionItem item = new CheckoutSessionItem();
        item.id = id;
        item.checkoutSessionId = checkoutSessionId;
        item.sourceCartItemId = sourceCartItemId;
        item.sourceCartItemVersion = sourceCartItemVersion;
        item.productId = productId;
        item.variantId = variantId;
        item.sku = sku;
        item.productName = productName;
        item.variantName = variantName;
        item.imageUrl = imageUrl;
        item.listPriceVnd = listPriceVnd;
        item.directSalePromotionId = directSalePromotionId;
        item.directSaleDiscountVnd = directSaleDiscountVnd;
        item.unitPriceVnd = Math.subtractExact(listPriceVnd, directSaleDiscountVnd);
        item.quantity = quantity;
        item.weightGrams = weightGrams;
        item.lengthCm = lengthCm;
        item.widthCm = widthCm;
        item.heightCm = heightCm;
        item.createdAt = now;
        return item;
    }
}
