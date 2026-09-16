package com.dynamicmart.catalog_service.entity;

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
@Table(name = "inventory_items")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {
    @Id @Column(name = "variant_id") private UUID variantId;
    @Column(name = "on_hand_qty", nullable = false) private int onHandQty;
    @Column(name = "reserved_qty", nullable = false) private int reservedQty;
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
