package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import java.util.UUID;

public record CategoryAttributeResponse(
        UUID id,
        UUID categoryId,
        UUID attributeId,
        String attributeCode,
        String attributeName,
        AttributeAppliesTo appliesTo,
        boolean required,
        boolean filterable,
        int sortOrder
) {
}
