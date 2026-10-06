package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {
    @Id private UUID id;
    @Column(name = "wishlist_id", nullable = false) private UUID wishlistId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected WishlistItem() { }
    public WishlistItem(UUID wishlistId, UUID productId, Instant now) {
        this.id = UUID.randomUUID(); this.wishlistId = wishlistId; this.productId = productId; this.createdAt = now;
    }
    public UUID getProductId() { return productId; }
    public Instant getCreatedAt() { return createdAt; }
}
