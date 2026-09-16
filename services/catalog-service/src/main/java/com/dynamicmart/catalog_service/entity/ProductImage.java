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
@Table(name = "product_images")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "variant_id") private UUID variantId;
    @Column(name = "image_url", nullable = false, length = 1000) private String imageUrl;
    @Column(name = "alt_text", length = 300) private String altText;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "is_primary", nullable = false) private boolean primary;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
