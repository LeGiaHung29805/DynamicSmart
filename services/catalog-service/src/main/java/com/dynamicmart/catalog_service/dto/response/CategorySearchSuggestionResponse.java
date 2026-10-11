package com.dynamicmart.catalog_service.dto.response;

import java.util.UUID;

public record CategorySearchSuggestionResponse(UUID id, String name, String slug) {
}
