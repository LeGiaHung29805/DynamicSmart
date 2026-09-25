package com.dynamicmart.cart_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class PromotionDtos {
    private PromotionDtos() { }
    public record DirectSaleRequest(
            @NotBlank @Size(max = 200) String name, @Size(max = 2000) String description,
            @NotBlank String discountMethod, @PositiveOrZero Long fixedDiscountVnd,
            @Positive Integer discountRateBps, @PositiveOrZero Long maxDiscountVnd,
            @NotNull Instant startsAt, @NotNull @Future Instant endsAt,
            @NotEmpty Set<UUID> variantIds, @NotBlank @Size(max = 500) String reason) { }
    public record StatusRequest(@NotBlank String status, @NotBlank @Size(max = 500) String reason) { }
    public record DirectSaleResponse(UUID id, String name, String description, String status, String discountMethod,
                                     Long fixedDiscountVnd, Integer discountRateBps, Long maxDiscountVnd,
                                     Instant startsAt, Instant endsAt, Set<UUID> variantIds, Instant updatedAt) { }
    public record DirectSalePriceResponse(UUID variantId, long listPriceVnd, long discountVnd, long salePriceVnd,
                                          Integer discountPercent, UUID promotionId, String promotionName, Instant endsAt) { }
    public record PromotionAuditResponse(UUID id, UUID actorAdminId, String targetType, UUID targetId,
                                         String action, String reason, Instant createdAt) { }
}
