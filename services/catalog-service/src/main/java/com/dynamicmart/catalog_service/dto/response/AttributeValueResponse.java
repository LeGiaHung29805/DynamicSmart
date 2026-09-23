package com.dynamicmart.catalog_service.dto.response;

import com.dynamicmart.catalog_service.entity.AttributeDataType;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record AttributeValueResponse(
        UUID id,
        UUID attributeId,
        String attributeCode,
        String attributeName,
        AttributeDataType dataType,
        JsonNode value
) {
}
