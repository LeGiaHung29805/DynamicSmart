package com.dynamicmart.catalog_service.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CommitInventoryRequest(@NotNull UUID orderId) {
}
