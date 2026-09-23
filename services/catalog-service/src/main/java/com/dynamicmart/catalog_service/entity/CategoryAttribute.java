package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_attributes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryAttribute {
    @Id private UUID id;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(name = "attribute_id", nullable = false) private UUID attributeId;
    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false, length = 20) private AttributeAppliesTo appliesTo;
    @Column(name = "is_required", nullable = false) private boolean required;
    @Column(name = "is_filterable", nullable = false) private boolean filterable;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public CategoryAttribute(UUID id, UUID categoryId, UUID attributeId, AttributeAppliesTo appliesTo,
                             boolean required, boolean filterable, int sortOrder) {
        this.id = id;
        this.categoryId = categoryId;
        this.attributeId = attributeId;
        this.appliesTo = appliesTo;
        this.required = required;
        this.filterable = filterable;
        this.sortOrder = sortOrder;
    }

    public void update(boolean required, boolean filterable, int sortOrder) {
        this.required = required;
        this.filterable = filterable;
        this.sortOrder = sortOrder;
    }
}
