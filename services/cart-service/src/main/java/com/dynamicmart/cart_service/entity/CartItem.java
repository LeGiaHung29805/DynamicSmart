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
import lombok.Setter;

@Entity
@Table(name = "cart_items")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {
    @Id private UUID id;
    @Column(name = "cart_id", nullable = false) private UUID cartId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private long version;
    @Column(name = "is_selected", nullable = false) private boolean selected;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
