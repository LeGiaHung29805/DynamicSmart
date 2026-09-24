package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "order_id") private UUID orderId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private InventoryReservationStatus status;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "committed_at") private Instant committedAt;
    @Column(name = "released_at") private Instant releasedAt;
    @Column(name = "release_reason", length = 80) private String releaseReason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public InventoryReservation(UUID id, UUID checkoutSessionId, Instant expiresAt) {
        this.id = id;
        this.checkoutSessionId = checkoutSessionId;
        this.expiresAt = expiresAt;
        this.status = InventoryReservationStatus.RESERVED;
    }

    public void attachOrder(UUID orderId) { this.orderId = orderId; }

    public void commit(UUID orderId, Instant now) {
        requireReserved();
        this.orderId = orderId;
        status = InventoryReservationStatus.COMMITTED;
        committedAt = now;
    }

    public void release(String reason, Instant now) {
        requireReserved();
        status = InventoryReservationStatus.RELEASED;
        releaseReason = reason;
        releasedAt = now;
    }

    public void expire(Instant now) {
        requireReserved();
        status = InventoryReservationStatus.EXPIRED;
        releaseReason = "RESERVATION_EXPIRED";
        releasedAt = now;
    }

    public boolean isExpiredAt(Instant now) { return !expiresAt.isAfter(now); }

    private void requireReserved() {
        if (status != InventoryReservationStatus.RESERVED) {
            throw new IllegalStateException("Inventory reservation is not active");
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = InventoryReservationStatus.RESERVED;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
