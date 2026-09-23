package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.NotNull;

public record CatalogStatusRequest(@NotNull Boolean active) {
}
