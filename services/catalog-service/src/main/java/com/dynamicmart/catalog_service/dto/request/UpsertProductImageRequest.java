package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpsertProductImageRequest(
        UUID variantId,
        @NotBlank @Size(max = 1000) String imageUrl,
        @NotBlank @Size(max = 100) String contentType,
        @Positive long sizeBytes,
        @Size(max = 300) String altText,
        @Min(0) int sortOrder,
        boolean primary
) {
}
