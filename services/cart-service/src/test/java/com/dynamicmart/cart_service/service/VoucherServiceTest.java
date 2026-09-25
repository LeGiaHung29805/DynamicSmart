package com.dynamicmart.cart_service.service;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;
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

    private Voucher voucher(UUID id) {
        Instant now = Instant.now();
        Voucher value = new Voucher("SAVE10", "Save", null, "ORDER_DISCOUNT", "FIXED_AMOUNT", 10_000L,
                null, null, 0L, 0L, 100, 1, now.minusSeconds(60), now.plus(1, ChronoUnit.DAYS),
                "CODE_ONLY", false, Set.of(), Set.of(), UUID.randomUUID(), now);
        value.setId(id); value.changeStatus("ACTIVE", now); return value;
    }
}
