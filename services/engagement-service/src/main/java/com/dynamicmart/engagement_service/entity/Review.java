package com.dynamicmart.engagement_service.entity;

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
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {
    @Id private UUID id;
    @Column(name = "order_item_id", nullable = false) private UUID orderItemId;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(nullable = false) private short rating;
    private String content;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "hidden_reason", length = 500) private String hiddenReason;
    @Column(name = "hidden_by") private UUID hiddenBy;
    @Column(name = "hidden_at") private Instant hiddenAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public Review(UUID orderItemId, UUID orderId, UUID customerId, UUID productId,
                  UUID variantId, short rating, String content, Instant now) {
        this.id = UUID.randomUUID();
        this.orderItemId = orderItemId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.productId = productId;
        this.variantId = variantId;
        this.rating = rating;
        this.content = content;
        this.status = "VISIBLE";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void hide(UUID adminId, String reason, Instant now) {
        this.status = "HIDDEN";
        this.hiddenBy = adminId;
        this.hiddenReason = reason;
        this.hiddenAt = now;
        this.updatedAt = now;
    }
}
