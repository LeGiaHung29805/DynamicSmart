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
    public record CheckoutSelectionItem(UUID cartItemId, long cartItemVersion, UUID productId,
                                        UUID variantId, int quantity) { }
    public record CheckoutSelectionResponse(UUID cartId, List<CheckoutSelectionItem> items) { }
    public record PricingItem(@NotNull UUID productId, @NotNull UUID variantId, @Positive int quantity,
                              @PositiveOrZero long unitPriceVnd) { }
    public record VoucherPricingRequest(@NotNull UUID customerId, UUID merchandiseVoucherId,
                                        UUID shippingVoucherId, @PositiveOrZero long shippingFeeVnd,
                                        @NotEmpty List<@Valid PricingItem> items) { }
    public record AppliedVoucher(UUID voucherId, String voucherCode, String scope, String discountMethod,
                                 Long discountValue, long eligibleSubtotalVnd, long discountAmountVnd,
                                 long shippingDiscountVnd) { }
    public record LineDiscount(UUID variantId, long productDiscountVnd, long orderDiscountVnd) { }
    public record VoucherPricingResponse(List<AppliedVoucher> vouchers, List<LineDiscount> lineDiscounts) { }
    public record ReservationBenefit(@NotNull UUID voucherId, @NotBlank String code, @NotBlank String scope,
                                     @NotBlank String discountMethod, Long discountValue,
                                     @PositiveOrZero long eligibleSubtotalVnd,
                                     @PositiveOrZero long discountAmountVnd,
                                     @PositiveOrZero long shippingDiscountVnd) { }
    public record ReservationLine(@NotNull UUID productId, @NotNull UUID variantId, @Positive int quantity,
                                  @PositiveOrZero long unitPriceVnd, @PositiveOrZero long productDiscountVnd,
                                  @PositiveOrZero long orderDiscountVnd) { }
    public record ReserveVoucherGroupRequest(@NotNull UUID operationKey, @NotNull UUID sagaId,
                                             UUID correlationId, @NotNull UUID customerId,
                                             @NotNull UUID checkoutSessionId,
                                             @PositiveOrZero long shippingFeeVnd,
                                             @NotNull @Future Instant reservedUntil,
                                             @NotEmpty List<@Valid ReservationBenefit> vouchers,
                                             @NotEmpty List<@Valid ReservationLine> lines) { }
    public record VoucherGroupResponse(UUID reservationId) { }
}
