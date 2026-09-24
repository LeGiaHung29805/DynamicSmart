package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private VariantStatus status;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Version
    @Column(nullable = false) private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ProductVariant(UUID id, UUID productId, String sku, String name, long priceVnd,
                          int weightGrams, Integer lengthCm, Integer widthCm, Integer heightCm,
                          int sortOrder) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.priceVnd = priceVnd;
        this.weightGrams = weightGrams;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.status = VariantStatus.INACTIVE;
        this.sortOrder = sortOrder;
    }

    public void update(String sku, String name, long priceVnd, int weightGrams,
                       Integer lengthCm, Integer widthCm, Integer heightCm, int sortOrder) {
        this.sku = sku;
        this.name = name;
        this.priceVnd = priceVnd;
        this.weightGrams = weightGrams;
        this.lengthCm = lengthCm;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.sortOrder = sortOrder;
    }

    public void activate() { status = VariantStatus.ACTIVE; }
    public void deactivate() { status = VariantStatus.INACTIVE; }
    public void archive() { status = VariantStatus.ARCHIVED; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = VariantStatus.INACTIVE;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
