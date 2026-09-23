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
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    @Id private UUID id;
    @Column(name = "parent_id") private UUID parentId;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 220) private String slug;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private CatalogStatus status;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public Category(UUID id, UUID parentId, String code, String name, String slug,
                    String description, int sortOrder) {
        this.id = id;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.status = CatalogStatus.ACTIVE;
        this.sortOrder = sortOrder;
    }

    public void update(UUID parentId, String code, String name, String slug,
                       String description, int sortOrder) {
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public void activate() { status = CatalogStatus.ACTIVE; }
    public void deactivate() { status = CatalogStatus.INACTIVE; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = CatalogStatus.ACTIVE;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
