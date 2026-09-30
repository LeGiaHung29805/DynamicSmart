package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateVariantRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String sku,
        @Size(max = 300) String name,
        @PositiveOrZero long priceVnd,
        @Positive int weightGrams,
        @Positive Integer lengthCm,
        @Positive Integer widthCm,
        @Positive Integer heightCm,
        @Min(0) int sortOrder,
        @PositiveOrZero int initialOnHandQuantity,
        List<@Valid AttributeValueRequest> attributes
) {
    public CreateVariantRequest {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
}
