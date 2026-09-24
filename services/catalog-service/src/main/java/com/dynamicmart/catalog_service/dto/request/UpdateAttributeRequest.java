package com.dynamicmart.catalog_service.dto.request;

import com.dynamicmart.catalog_service.entity.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record UpdateAttributeRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull AttributeDataType dataType,
        JsonNode validationConfig
) {
}
