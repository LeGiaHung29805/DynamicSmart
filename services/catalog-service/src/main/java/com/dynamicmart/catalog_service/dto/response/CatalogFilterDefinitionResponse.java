package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.AttributeAppliesTo;
import com.dynamicmart.catalog_service.entity.AttributeDataType;
import java.util.List;
import java.util.UUID;

public record CatalogFilterDefinitionResponse(
        UUID attributeId,
        String code,
        String name,
        AttributeDataType dataType,
        AttributeAppliesTo appliesTo,
        List<CatalogFilterOptionResponse> options
) {
}
