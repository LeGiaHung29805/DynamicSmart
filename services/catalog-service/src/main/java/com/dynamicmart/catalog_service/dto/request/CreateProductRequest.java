package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 300) String name,
        @Size(max = 330) String slug,
        @Size(max = 1000) String shortDescription,
        @Size(max = 20000) String description,
        @Positive Integer defaultWeightGrams,
        @Positive Integer defaultLengthCm,
        @Positive Integer defaultWidthCm,
        @Positive Integer defaultHeightCm,
        boolean featured,
        List<@Valid AttributeValueRequest> attributes
) {
    public CreateProductRequest {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
}
