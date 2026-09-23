package com.dynamicmart.catalog_service.query;

import java.util.List;
import java.util.UUID;

public record ProductIdPage(List<UUID> productIds, long totalElements) {
}
