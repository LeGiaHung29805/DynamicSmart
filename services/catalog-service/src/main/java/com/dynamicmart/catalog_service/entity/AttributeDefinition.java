package com.dynamicmart.catalog_service.entity;

import tools.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attributes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributeDefinition {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20) private AttributeDataType dataType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_config", columnDefinition = "jsonb") private JsonNode validationConfig;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private CatalogStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public AttributeDefinition(UUID id, String code, String name, AttributeDataType dataType,
                               JsonNode validationConfig) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.dataType = dataType;
        this.validationConfig = validationConfig;
        this.status = CatalogStatus.ACTIVE;
    }

    public void update(String code, String name, AttributeDataType dataType, JsonNode validationConfig) {
        this.code = code;
        this.name = name;
        this.dataType = dataType;
        this.validationConfig = validationConfig;
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
