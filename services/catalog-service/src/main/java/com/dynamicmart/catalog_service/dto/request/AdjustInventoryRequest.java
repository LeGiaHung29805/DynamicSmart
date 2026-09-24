package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
        @NotNull Integer quantityDelta,
        @NotBlank @Size(max = 500) String reason
) {
}
