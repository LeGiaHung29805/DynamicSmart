package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.CatalogStatus;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String code,
        String name,
        String slug,
        String description,
        CatalogStatus status,
        int sortOrder
) {
}
