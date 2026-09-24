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
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
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
    @Column(name = "product_discount_vnd", nullable = false) private long productDiscountVnd;
    @Column(name = "order_discount_vnd", nullable = false) private long orderDiscountVnd;
    @Column(name = "line_total_vnd", nullable = false) private long lineTotalVnd;
    @Column(name = "weight_grams", nullable = false) private int weightGrams;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static OrderItem create(
            UUID id,
            UUID orderId,
            UUID productId,
            UUID variantId,
            String sku,
            String productName,
            String variantName,
            String imageUrl,
            long listPriceVnd,
            UUID directSalePromotionId,
            long directSaleDiscountVnd,
            long unitPriceVnd,
            int quantity,
            long productDiscountVnd,
            long orderDiscountVnd,
            long lineTotalVnd,
            int weightGrams,
            Instant now) {
        OrderItem item = new OrderItem();
        item.id = id;
        item.orderId = orderId;
        item.productId = productId;
        item.variantId = variantId;
        item.sku = sku;
        item.productName = productName;
        item.variantName = variantName;
        item.imageUrl = imageUrl;
        item.listPriceVnd = listPriceVnd;
        item.directSalePromotionId = directSalePromotionId;
        item.directSaleDiscountVnd = directSaleDiscountVnd;
        item.unitPriceVnd = unitPriceVnd;
        item.quantity = quantity;
        item.productDiscountVnd = productDiscountVnd;
        item.orderDiscountVnd = orderDiscountVnd;
        item.lineTotalVnd = lineTotalVnd;
        item.weightGrams = weightGrams;
        item.createdAt = now;
        return item;
    }
}
