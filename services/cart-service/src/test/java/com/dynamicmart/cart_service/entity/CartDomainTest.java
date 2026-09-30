package com.dynamicmart.cart_service.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CartDomainTest {
    @Test
    void changingCartItemIncrementsVersionAndKeepsServerOwnedFields() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        CartItem item = new CartItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, createdAt);

        item.change(3, false, createdAt.plus(1, ChronoUnit.MINUTES));

        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.isSelected()).isFalse();
        assertThat(item.getVersion()).isEqualTo(1);
        assertThat(item.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    void directSaleNeverProducesNegativePriceAndHonorsCap() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        DirectPricePromotion percentage = new DirectPricePromotion(
                "Sale", null, "PERCENTAGE", null, 2_500, 20_000L,
                now, now.plus(1, ChronoUnit.DAYS), UUID.randomUUID(), java.util.Set.of(UUID.randomUUID()), now);
        DirectPricePromotion fixed = new DirectPricePromotion(
                "Clearance", null, "FIXED_AMOUNT", 200_000L, null, null,
                now, now.plus(1, ChronoUnit.DAYS), UUID.randomUUID(), java.util.Set.of(UUID.randomUUID()), now);

        assertThat(percentage.discount(100_000)).isEqualTo(20_000);
        assertThat(fixed.discount(100_000)).isEqualTo(100_000);
    }

    @Test
    void voucherDiscountIsCappedByEligibleAmountAndConfiguredMaximum() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Voucher voucher = new Voucher("SAVE50", "Save", null, "ORDER_DISCOUNT", "PERCENTAGE",
                null, 5_000, 30_000L, 0L, 0L, 100, 1, now.minusSeconds(1),
                now.plus(1, ChronoUnit.DAYS), "CODE_ONLY", false, Set.of(), Set.of(), UUID.randomUUID(), now);
        voucher.changeStatus("ACTIVE", now);

        assertThat(voucher.activeAt(now)).isTrue();
        assertThat(voucher.discount(100_000)).isEqualTo(30_000);
        assertThat(voucher.discount(10_000)).isEqualTo(5_000);
    }

    @Test
    void expiredReservationReleasesQuotaOnlyOnce() {
        Instant now = Instant.now();
        VoucherReservation reservation = new VoucherReservation(UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), 10_000, 0, now.minusSeconds(1), now.minusSeconds(60));
        reservation.expire(now); reservation.expire(now.plusSeconds(1));
        assertThat(reservation.getStatus()).isEqualTo("EXPIRED");
        assertThat(reservation.getReleaseReason()).isEqualTo("RESERVATION_EXPIRED");
    }
}
