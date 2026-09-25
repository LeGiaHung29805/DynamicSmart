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
@Table(name = "carts")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public Cart(UUID customerId, Instant now) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.status = "ACTIVE";
        this.createdAt = now; this.updatedAt = now;
    }
    public void touch(Instant now) { this.updatedAt = now; }
}
