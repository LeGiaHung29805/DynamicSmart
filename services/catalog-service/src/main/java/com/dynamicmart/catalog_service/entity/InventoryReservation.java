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
@Table(name = "inventory_reservations")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservation {
    @Id private UUID id;
    @Column(name = "checkout_session_id", nullable = false) private UUID checkoutSessionId;
    @Column(name = "order_id") private UUID orderId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "committed_at") private Instant committedAt;
    @Column(name = "released_at") private Instant releasedAt;
    @Column(name = "release_reason", length = 80) private String releaseReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
