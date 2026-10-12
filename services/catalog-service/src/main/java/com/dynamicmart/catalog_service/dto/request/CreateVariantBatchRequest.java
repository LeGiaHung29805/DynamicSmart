package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateVariantBatchRequest(
        @NotEmpty @Size(max = 100) List<@Valid CreateVariantRequest> variants
) {
    public CreateVariantBatchRequest {
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
