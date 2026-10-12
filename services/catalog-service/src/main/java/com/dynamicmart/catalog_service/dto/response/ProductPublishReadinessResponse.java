package com.dynamicmart.catalog_service.dto.response;

import java.util.List;

public record ProductPublishReadinessResponse(
        boolean ready,
        List<ProductPublishCheckResponse> checks
) {
    public ProductPublishReadinessResponse {
        checks = List.copyOf(checks);
    }
}
