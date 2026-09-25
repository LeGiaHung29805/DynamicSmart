package com.dynamicmart.cart_service.service;

import com.dynamicmart.cart_service.repository.VoucherReservationRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherReservationExpiryService {
    private final VoucherReservationRepository reservations;
    public VoucherReservationExpiryService(VoucherReservationRepository reservations) { this.reservations = reservations; }
    @Scheduled(fixedDelayString = "${app.voucher.expiry-scan-ms:30000}")
    @Transactional
    public void expireReservations() {
        Instant now = Instant.now();
        reservations.findAllByStatusAndReservedUntilLessThanEqual("RESERVED", now).forEach(value -> value.expire(now));
    }
}
