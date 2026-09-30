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
@Table(name = "variant_attribute_values")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VariantAttributeValue {
    @Id private UUID id;
    @Column(name = "variant_id", nullable = false) private UUID variantId;
    @Column(name = "attribute_id", nullable = false) private UUID attributeId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "jsonb") private JsonNode value;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public VariantAttributeValue(UUID id, UUID variantId, UUID attributeId, JsonNode value) {
        this.id = id;
        this.variantId = variantId;
        this.attributeId = attributeId;
        this.value = value;
    }

    public void changeValue(JsonNode value) { this.value = value; }

    @PrePersist
    void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
