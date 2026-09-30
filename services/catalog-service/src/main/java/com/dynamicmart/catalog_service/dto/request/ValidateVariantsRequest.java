package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ValidateVariantsRequest(
        @NotEmpty @Size(max = 100) List<@Valid InventoryReservationItemRequest> items
) {
    public ValidateVariantsRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
