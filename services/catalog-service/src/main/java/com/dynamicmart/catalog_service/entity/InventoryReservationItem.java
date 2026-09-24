package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_reservation_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryReservationItem {
    @Id private UUID id;
    @Column(name = "reservation_id", nullable = false) private UUID reservationId;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(nullable = false) private int quantity;

    public InventoryReservationItem(UUID id, UUID reservationId, UUID variantId, int quantity) {
        this.id = id;
        this.reservationId = reservationId;
        this.variantId = variantId;
        this.quantity = quantity;
    }
}
