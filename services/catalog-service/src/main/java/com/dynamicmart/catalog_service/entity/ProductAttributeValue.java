package com.dynamicmart.catalog_service.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "product_attribute_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductAttributeValue {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "attribute_id", nullable = false) private UUID attributeId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "jsonb") private JsonNode value;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ProductAttributeValue(UUID id, UUID productId, UUID attributeId, JsonNode value) {
        this.id = id;
        this.productId = productId;
        this.attributeId = attributeId;
        this.value = value;
    }

    public void changeValue(JsonNode value) { this.value = value; }

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
