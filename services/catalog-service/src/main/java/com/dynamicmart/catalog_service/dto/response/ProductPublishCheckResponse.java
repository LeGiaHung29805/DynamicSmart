package com.dynamicmart.catalog_service.dto.response;

public record ProductPublishCheckResponse(
        String code,
        String label,
        boolean complete,
        String message
) {
}
