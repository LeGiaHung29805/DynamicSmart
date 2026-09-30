package com.dynamicmart.cart_service.entity;

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
@Table(name = "promotion_audits")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionAudit {
    @Id private UUID id;
    @Column(name = "actor_admin_id", nullable = false) private UUID actorAdminId;
    @Column(name = "target_type", nullable = false, length = 30) private String targetType;
    @Column(name = "target_id", nullable = false) private UUID targetId;
    @Column(nullable = false, length = 50) private String action;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public PromotionAudit(UUID actorAdminId, String targetType, UUID targetId, String action, String reason,
                          UUID idempotencyKey, Instant now) {
        this.id = UUID.randomUUID(); this.actorAdminId = actorAdminId; this.targetType = targetType;
        this.targetId = targetId; this.action = action; this.reason = reason;
        this.idempotencyKey = idempotencyKey; this.createdAt = now;
    }
}
