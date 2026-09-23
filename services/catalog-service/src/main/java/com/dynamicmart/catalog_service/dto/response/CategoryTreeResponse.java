package com.dynamicmart.catalog_service.dto.response;

import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
        UUID id,
        String code,
        String name,
        String slug,
        String description,
        int sortOrder,
        List<CategoryTreeResponse> children
) {
}
