package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;
import static com.dynamicmart.cart_service.dto.VoucherDtos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dynamicmart.cart_service.entity.Voucher;
import com.dynamicmart.cart_service.entity.VoucherReservation;
import com.dynamicmart.cart_service.exception.CartException;
import com.dynamicmart.cart_service.repository.CustomerVoucherRepository;
import com.dynamicmart.cart_service.repository.PromotionAuditRepository;
import com.dynamicmart.cart_service.repository.VoucherRepository;
import com.dynamicmart.cart_service.repository.VoucherReservationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoucherServiceTest {
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final CustomerVoucherRepository wallets = mock(CustomerVoucherRepository.class);
    private final VoucherReservationRepository reservations = mock(VoucherReservationRepository.class);
    private final PromotionAuditRepository audits = mock(PromotionAuditRepository.class);
    private final VoucherService service = new VoucherService(vouchers, wallets, reservations, audits);

    @Test
    void repeatedReserveReturnsSameReservationWithoutAllocatingAgain() {
        UUID voucherId = UUID.randomUUID(), customer = UUID.randomUUID(), checkout = UUID.randomUUID();
        Voucher voucher = voucher(voucherId); VoucherReservation existing = new VoucherReservation(voucherId, customer, null, checkout, 10_000, 0, Instant.now().plusSeconds(60), Instant.now());
        when(vouchers.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(reservations.findByCheckoutSessionIdAndVoucherId(checkout, voucherId)).thenReturn(Optional.of(existing));
        var request = new ReserveVoucherRequest(voucherId, customer, checkout, 100_000, 100_000, 0, Set.of(), Set.of(), Instant.now().plusSeconds(60));

        var response = service.reserve(request);

        assertThat(response.id()).isEqualTo(existing.getId()); verify(reservations, never()).save(any());
    }

    @Test
    void consumedReservationCannotBeReusedForAnotherOrder() {
        UUID voucherId = UUID.randomUUID(), customer = UUID.randomUUID(), checkout = UUID.randomUUID();
        VoucherReservation existing = new VoucherReservation(voucherId, customer, null, checkout, 10_000, 0, Instant.now().plusSeconds(60), Instant.now());
        UUID firstOrder = UUID.randomUUID(); existing.consume(firstOrder, Instant.now());
        when(reservations.findByIdForUpdate(existing.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.consume(existing.getId(), UUID.randomUUID()))
                .isInstanceOf(CartException.class).hasMessageContaining("đơn hàng khác");
    }

    @Test
    void releasingSameReservationTwiceIsSafe() {
        UUID voucherId = UUID.randomUUID(), customer = UUID.randomUUID(), checkout = UUID.randomUUID();
        VoucherReservation existing = new VoucherReservation(voucherId, customer, null, checkout, 10_000, 0, Instant.now().plusSeconds(60), Instant.now());
        when(reservations.findByIdForUpdate(existing.getId())).thenReturn(Optional.of(existing));

        service.release(existing.getId(), "PAYMENT_FAILED");
        var repeated = service.release(existing.getId(), "PAYMENT_FAILED");

        assertThat(repeated.status()).isEqualTo("RELEASED");
        assertThat(existing.getReleaseReason()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void exhaustedGlobalQuotaCannotBeReserved() {
        UUID voucherId = UUID.randomUUID(), customer = UUID.randomUUID(), checkout = UUID.randomUUID();
        Voucher voucher = voucher(voucherId);
        when(vouchers.findById(voucherId)).thenReturn(Optional.of(voucher));
        when(vouchers.findByIdForUpdate(voucherId)).thenReturn(Optional.of(voucher));
        when(reservations.findByCheckoutSessionIdAndVoucherId(checkout, voucherId)).thenReturn(Optional.empty());
        when(reservations.countAllocated(voucherId)).thenReturn(100L);
        var request = new ReserveVoucherRequest(voucherId, customer, checkout, 100_000, 100_000, 0, Set.of(), Set.of(), Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> service.reserve(request)).isInstanceOf(CartException.class).hasMessageContaining("hết lượt");
        verify(reservations, never()).save(any());
    }

    @Test
    void expiredVoucherIsIneligible() {
        UUID voucherId = UUID.randomUUID(); Instant now = Instant.now();
        Voucher expired = new Voucher("OLD", "Expired", null, "ORDER_DISCOUNT", "FIXED_AMOUNT", 10_000L,
                null, null, 0L, 0L, 10, 1, now.minusSeconds(120), now.minusSeconds(60),
                "CODE_ONLY", false, Set.of(), Set.of(), UUID.randomUUID(), now.minusSeconds(120));
        expired.setId(voucherId); expired.changeStatus("ACTIVE", now);
        when(vouchers.findById(voucherId)).thenReturn(Optional.of(expired));

        var result = service.preview(UUID.randomUUID(), new VoucherPreviewRequest(voucherId, "OLD", 100_000, 100_000, 0, Set.of(), Set.of()));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("hết hạn");
    }

    @Test
    void minimumOrderConditionIsEvaluatedOnServer() {
        UUID voucherId = UUID.randomUUID(); Voucher voucher = voucher(voucherId); voucher.setMinimumOrderVnd(200_000L);
        when(vouchers.findById(voucherId)).thenReturn(Optional.of(voucher));

        var result = service.preview(UUID.randomUUID(), new VoucherPreviewRequest(voucherId, "SAVE10", 100_000, 100_000, 0, Set.of(), Set.of()));

        assertThat(result.eligible()).isFalse();
        assertThat(result.ineligibleReason()).contains("tối thiểu");
    }

    @Test
    void customerHistoryOnlyReadsReservationsOwnedByAuthenticatedCustomer() {
        UUID customer = UUID.randomUUID(), voucherId = UUID.randomUUID(), checkout = UUID.randomUUID();
        Voucher voucher = voucher(voucherId);
        VoucherReservation reservation = new VoucherReservation(voucherId, customer, null, checkout,
                10_000, 0, Instant.now().plusSeconds(60), Instant.now());
        when(reservations.findAllByCustomerIdOrderByCreatedAtDesc(customer)).thenReturn(List.of(reservation));
        when(vouchers.findById(voucherId)).thenReturn(Optional.of(voucher));

        var result = service.customerHistory(customer);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).voucherCode()).isEqualTo("SAVE10");
        verify(reservations).findAllByCustomerIdOrderByCreatedAtDesc(customer);
        verify(reservations, never()).findAllByOrderByCreatedAtDesc();
    }

    private Voucher voucher(UUID id) {
        Instant now = Instant.now();
        Voucher value = new Voucher("SAVE10", "Save", null, "ORDER_DISCOUNT", "FIXED_AMOUNT", 10_000L,
                null, null, 0L, 0L, 100, 1, now.minusSeconds(60), now.plus(1, ChronoUnit.DAYS),
                "CODE_ONLY", false, Set.of(), Set.of(), UUID.randomUUID(), now);
        value.setId(id); value.changeStatus("ACTIVE", now); return value;
    }
}
