package com.dynamicmart.cart_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class VoucherDtos {
    private VoucherDtos() { }
    public record VoucherRuleRequest(
            @NotBlank @Size(max = 80) String code, @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description, @NotBlank String scope, @NotBlank String discountMethod,
            @PositiveOrZero Long fixedDiscountVnd, @Positive Integer discountRateBps,
            @PositiveOrZero Long maxDiscountVnd, @PositiveOrZero Long minimumOrderVnd,
            @PositiveOrZero Long minimumEligibleSubtotalVnd, @Positive Integer usageLimit,
            @Positive Integer usageLimitPerCustomer, @NotNull Instant startsAt, @NotNull @Future Instant endsAt,
            @NotBlank String distributionMode, boolean defaultVoucher, Set<UUID> productIds,
            Set<UUID> categoryIds, @NotBlank @Size(max = 500) String reason) { }
    public record VoucherStatusRequest(@NotBlank String status, @NotBlank @Size(max = 500) String reason) { }
    public record AssignmentRequest(@NotNull UUID customerId, Instant expiresAt,
                                    @NotBlank @Size(max = 500) String reason) { }
    public record VoucherPreviewRequest(UUID voucherId, String code, @PositiveOrZero long orderSubtotalVnd,
                                        @PositiveOrZero long eligibleSubtotalVnd, @PositiveOrZero long shippingFeeVnd,
                                        Set<UUID> productIds, Set<UUID> categoryIds) { }
    public record VoucherResponse(UUID id, String code, String name, String description, String status, String scope,
                                  String discountMethod, Long fixedDiscountVnd, Integer discountRateBps,
                                  Long maxDiscountVnd, Long minimumOrderVnd, Long minimumEligibleSubtotalVnd,
                                  Integer usageLimit, int consumedCount, Integer usageLimitPerCustomer,
                                  Instant startsAt, Instant endsAt, String distributionMode, boolean defaultVoucher,
                                  Set<UUID> productIds, Set<UUID> categoryIds, boolean eligible, String ineligibleReason,
                                  long discountAmountVnd, UUID customerVoucherId) { }
    public record AssignmentResponse(UUID id, UUID customerId, UUID voucherId, String status,
                                     Instant assignedAt, Instant expiresAt) { }
}
