package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record CategoryBriefResponse(
        UUID id,
        String code,
        String name,
        String slug
) {
}
