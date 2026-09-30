package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_adjustments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryAdjustment {
    @Id private UUID id;
    @Column(name = "operation_key", nullable = false, unique = true) private UUID operationKey;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(name = "quantity_delta", nullable = false) private int quantityDelta;
    @Column(name = "on_hand_before", nullable = false) private int onHandBefore;
    @Column(name = "on_hand_after", nullable = false) private int onHandAfter;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "actor_admin_id", nullable = false) private UUID actorAdminId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public InventoryAdjustment(UUID id, UUID operationKey, UUID variantId, int quantityDelta,
                               int onHandBefore, int onHandAfter, String reason, UUID actorAdminId) {
        this.id = id;
        this.operationKey = operationKey;
        this.variantId = variantId;
        this.quantityDelta = quantityDelta;
        this.onHandBefore = onHandBefore;
        this.onHandAfter = onHandAfter;
        this.reason = reason;
        this.actorAdminId = actorAdminId;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
