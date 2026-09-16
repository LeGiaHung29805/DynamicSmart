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
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false, length = 100) private String sku;
    @Column(length = 300) private String name;
    @Column(name = "price_vnd", nullable = false) private long priceVnd;
    @Column(name = "weight_grams", nullable = false) private int weightGrams;
    @Column(name = "length_cm") private Integer lengthCm;
    @Column(name = "width_cm") private Integer widthCm;
    @Column(name = "height_cm") private Integer heightCm;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
