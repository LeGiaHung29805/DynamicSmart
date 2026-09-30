package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseInventoryRequest(
        @NotBlank @Size(max = 80) String reason
) {
}
