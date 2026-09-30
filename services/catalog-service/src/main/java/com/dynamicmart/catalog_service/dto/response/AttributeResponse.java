package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.AttributeDataType;
import com.dynamicmart.catalog_service.entity.CatalogStatus;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record AttributeResponse(
        UUID id,
        String code,
        String name,
        AttributeDataType dataType,
        JsonNode validationConfig,
        CatalogStatus status,
        List<AttributeOptionResponse> options
) {
}
