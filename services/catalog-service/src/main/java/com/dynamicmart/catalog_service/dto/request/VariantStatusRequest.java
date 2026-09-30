package com.dynamicmart.catalog_service.dto.request;

import com.dynamicmart.catalog_service.entity.VariantStatus;
import jakarta.validation.constraints.NotNull;

public record VariantStatusRequest(@NotNull VariantStatus status) {
}
