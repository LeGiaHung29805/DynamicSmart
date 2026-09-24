package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.CatalogStatus;
import java.util.UUID;

public record AttributeOptionResponse(
        UUID id,
        UUID attributeId,
        String code,
        String label,
        int sortOrder,
        CatalogStatus status
) {
}
