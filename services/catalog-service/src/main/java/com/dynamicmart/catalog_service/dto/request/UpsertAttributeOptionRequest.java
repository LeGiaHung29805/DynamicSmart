package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertAttributeOptionRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code,
        @NotBlank @Size(max = 120) String label,
        @Min(0) int sortOrder
) {
}
