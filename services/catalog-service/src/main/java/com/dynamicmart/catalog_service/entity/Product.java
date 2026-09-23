package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id private UUID id;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(nullable = false, length = 300) private String name;
    @Column(nullable = false, length = 330) private String slug;
    @Column(name = "short_description", length = 1000) private String shortDescription;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private ProductStatus status;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "is_featured", nullable = false) private boolean featured;
    @Column(name = "default_weight_grams") private Integer defaultWeightGrams;
    @Column(name = "default_length_cm") private Integer defaultLengthCm;
    @Column(name = "default_width_cm") private Integer defaultWidthCm;
    @Column(name = "default_height_cm") private Integer defaultHeightCm;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public Product(UUID id, UUID categoryId, String name, String slug, String shortDescription,
                   String description, Integer defaultWeightGrams, Integer defaultLengthCm,
                   Integer defaultWidthCm, Integer defaultHeightCm) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.status = ProductStatus.DRAFT;
        this.defaultWeightGrams = defaultWeightGrams;
        this.defaultLengthCm = defaultLengthCm;
        this.defaultWidthCm = defaultWidthCm;
        this.defaultHeightCm = defaultHeightCm;
    }

    public void update(UUID categoryId, String name, String slug, String shortDescription,
                       String description, Integer defaultWeightGrams, Integer defaultLengthCm,
                       Integer defaultWidthCm, Integer defaultHeightCm) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        this.defaultWeightGrams = defaultWeightGrams;
        this.defaultLengthCm = defaultLengthCm;
        this.defaultWidthCm = defaultWidthCm;
        this.defaultHeightCm = defaultHeightCm;
    }

    public void publish(Instant publicationTime) {
        status = ProductStatus.ACTIVE;
        publishedAt = publicationTime == null ? Instant.now() : publicationTime;
    }

    public void deactivate() { status = ProductStatus.INACTIVE; }
    public void archive() { status = ProductStatus.ARCHIVED; }
    public void markFeatured(boolean value) { featured = value; }

    public boolean isPublicAt(Instant now) {
        return status == ProductStatus.ACTIVE && publishedAt != null && !publishedAt.isAfter(now);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ProductStatus.DRAFT;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
