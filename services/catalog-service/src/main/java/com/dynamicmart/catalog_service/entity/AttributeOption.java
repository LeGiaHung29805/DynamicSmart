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
@Table(name = "attribute_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributeOption {
    @Id private UUID id;
    @Column(name = "attribute_id", nullable = false) private UUID attributeId;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 120) private String label;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private CatalogStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public AttributeOption(UUID id, UUID attributeId, String code, String label, int sortOrder) {
        this.id = id;
        this.attributeId = attributeId;
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
        this.status = CatalogStatus.ACTIVE;
    }

    public void update(String code, String label, int sortOrder) {
        this.code = code;
        this.label = label;
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
