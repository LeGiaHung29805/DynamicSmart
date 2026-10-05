package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
public class Wishlist {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false, unique = true) private UUID customerId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Wishlist() { }
    public Wishlist(UUID customerId, Instant now) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public void touch(Instant now) { this.updatedAt = now; }
}
