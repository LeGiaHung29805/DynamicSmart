package com.dynamicmart.catalog_service.dto.request;

import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MapCategoryAttributeRequest(
        @NotNull UUID attributeId,
        @NotNull AttributeAppliesTo appliesTo,
        boolean required,
        boolean filterable,
        @Min(0) int sortOrder
) {
}
