package com.dynamicmart.catalog_service.dto.request;

import com.dynamicmart.catalog_service.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ProductStatusRequest(
        @NotNull ProductStatus status,
        Instant publishedAt
) {
}
