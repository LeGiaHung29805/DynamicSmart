package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryItem {
    @Id
    @Column(name = "variant_id") private UUID variantId;
    @Column(name = "on_hand_qty", nullable = false) private int onHandQuantity;
    @Column(name = "reserved_qty", nullable = false) private int reservedQuantity;
    @Version
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public InventoryItem(UUID variantId, int onHandQuantity) {
        if (onHandQuantity < 0) throw new IllegalArgumentException("onHandQuantity must not be negative");
        this.variantId = variantId;
        this.onHandQuantity = onHandQuantity;
    }

    public int getAvailableQuantity() { return onHandQuantity - reservedQuantity; }

    public void reserve(int quantity) {
        requirePositive(quantity);
        if (getAvailableQuantity() < quantity) throw new IllegalStateException("Insufficient available inventory");
        reservedQuantity += quantity;
    }

    public void commit(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity || onHandQuantity < quantity) {
            throw new IllegalStateException("Reservation quantity is no longer valid");
        }
        reservedQuantity -= quantity;
        onHandQuantity -= quantity;
    }

    public void release(int quantity) {
        requirePositive(quantity);
        if (reservedQuantity < quantity) throw new IllegalStateException("Cannot release more than reserved quantity");
        reservedQuantity -= quantity;
    }

    public int adjust(int delta) {
        if (delta == 0) throw new IllegalArgumentException("Inventory adjustment must not be zero");
        int after = Math.addExact(onHandQuantity, delta);
        if (after < reservedQuantity) {
            throw new IllegalStateException("On-hand inventory cannot be below reserved inventory");
        }
        onHandQuantity = after;
        return after;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
