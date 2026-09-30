package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record AttributeValueRequest(
        @NotNull UUID attributeId,
        @NotNull JsonNode value
) {
}
