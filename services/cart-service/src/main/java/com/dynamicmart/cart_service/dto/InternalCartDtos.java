package com.dynamicmart.cart_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class InternalCartDtos {
    private InternalCartDtos() { }
    public record ReserveVoucherRequest(@NotNull UUID voucherId, @NotNull UUID customerId,
                                        @NotNull UUID checkoutSessionId, @PositiveOrZero long orderSubtotalVnd,
                                        @PositiveOrZero long eligibleSubtotalVnd, @PositiveOrZero long shippingFeeVnd,
                                        Set<UUID> productIds, Set<UUID> categoryIds,
                                        @NotNull @Future Instant reservedUntil) { }
    public record ReservationResponse(UUID id, UUID voucherId, UUID customerId, UUID checkoutSessionId,
                                      UUID orderId, String status, long discountAmountVnd,
                                      long shippingDiscountVnd, Instant reservedUntil) { }
    public record ConsumeVoucherRequest(@NotNull UUID orderId) { }
    public record ReleaseVoucherRequest(@NotBlank String reason) { }
    public record PurchasedCartItem(@NotNull UUID id, @Positive int quantity, @PositiveOrZero long version) { }
    public record OrderConfirmedRequest(@NotNull UUID eventId, UUID correlationId, @NotNull UUID customerId,
                                        @NotBlank String source, @NotEmpty List<@Valid PurchasedCartItem> items) { }
    public record CartCleanupResponse(int removedCount, boolean duplicate) { }
}
