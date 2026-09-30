package com.dynamicmart.catalog_service.dto.response;

public record InventoryAvailabilityResponse(
        int onHandQuantity,
        int reservedQuantity,
        int availableQuantity
) {
    public static InventoryAvailabilityResponse empty() {
        return new InventoryAvailabilityResponse(0, 0, 0);
    }
}
