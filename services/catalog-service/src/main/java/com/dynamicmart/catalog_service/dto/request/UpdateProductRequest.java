package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 300) String name,
        @NotBlank @Size(max = 330)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") String slug,
        @Size(max = 1000) String shortDescription,
        @Size(max = 20000) String description,
        @Positive Integer defaultWeightGrams,
        @Positive Integer defaultLengthCm,
        @Positive Integer defaultWidthCm,
        @Positive Integer defaultHeightCm,
        boolean featured,
        List<@Valid AttributeValueRequest> attributes
) {
    public UpdateProductRequest {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
}
