package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull UUID checkoutSessionId,
        @NotNull @Future Instant expiresAt,
        @NotEmpty @Size(max = 100) List<@Valid InventoryReservationItemRequest> items
) {
    public ReserveInventoryRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
