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
}
