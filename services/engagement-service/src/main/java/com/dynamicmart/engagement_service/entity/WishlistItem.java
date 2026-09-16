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
import lombok.Setter;

@Entity
@Table(name = "wishlist_items")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistItem {
    @Id private UUID id;
    @Column(name = "wishlist_id", nullable = false) private UUID wishlistId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
