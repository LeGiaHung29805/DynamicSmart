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
@Table(name = "products")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id private UUID id;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(nullable = false, length = 300) private String name;
    @Column(nullable = false, length = 330) private String slug;
    @Column(name = "short_description", length = 1000) private String shortDescription;
    private String description;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "default_weight_grams") private Integer defaultWeightGrams;
    @Column(name = "default_length_cm") private Integer defaultLengthCm;
    @Column(name = "default_width_cm") private Integer defaultWidthCm;
    @Column(name = "default_height_cm") private Integer defaultHeightCm;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
