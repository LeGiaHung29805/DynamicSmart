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
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {
    @Id private UUID id;
    @Column(name = "parent_id") private UUID parentId;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 220) private String slug;
    private String description;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
