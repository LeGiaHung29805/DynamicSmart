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

@Entity
@Table(name = "product_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductQuestion {
    @Id
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(nullable = false)
    private String content;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductQuestion(UUID productId, UUID customerId, String content, Instant now) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.customerId = customerId;
        this.content = content;
        this.status = "OPEN";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markAnswered(Instant now) {
        if (!"HIDDEN".equals(this.status)) {
            this.status = "ANSWERED";
            this.updatedAt = now;
        }
    }

    public void hide(String reason, Instant now) {
        this.status = "HIDDEN";
        this.hiddenReason = reason;
        this.updatedAt = now;
    }
}
