package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateCategoryRequest(
        UUID parentId,
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 220) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
        @Size(max = 5000) String description,
        @Min(0) int sortOrder
) {
}
