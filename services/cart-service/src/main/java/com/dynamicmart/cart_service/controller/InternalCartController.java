package com.dynamicmart.cart_service.controller;

import static com.dynamicmart.cart_service.dto.InternalCartDtos.*;

import com.dynamicmart.cart_service.service.CartCleanupService;
import com.dynamicmart.cart_service.service.InternalApiGuard;
import com.dynamicmart.cart_service.service.VoucherService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart/internal")
public class InternalCartController {
    private final InternalApiGuard guard; private final VoucherService vouchers; private final CartCleanupService cleanup;
    public InternalCartController(InternalApiGuard guard, VoucherService vouchers, CartCleanupService cleanup) {
        this.guard = guard; this.vouchers = vouchers; this.cleanup = cleanup;
    }
    @PostMapping("/voucher-reservations")
    public ReservationResponse reserve(@RequestHeader("X-Internal-Api-Key") String key,
                                       @Valid @RequestBody ReserveVoucherRequest request) {
        guard.verify(key); return vouchers.reserve(request);
    }
    @PostMapping("/voucher-reservations/{id}/consume")
    public ReservationResponse consume(@RequestHeader("X-Internal-Api-Key") String key, @PathVariable UUID id,
                                       @Valid @RequestBody ConsumeVoucherRequest request) {
        guard.verify(key); return vouchers.consume(id, request.orderId());
    }
    @PostMapping("/voucher-reservations/{id}/release")
    public ReservationResponse release(@RequestHeader("X-Internal-Api-Key") String key, @PathVariable UUID id,
                                       @Valid @RequestBody ReleaseVoucherRequest request) {
        guard.verify(key); return vouchers.release(id, request.reason());
    }
    @PostMapping("/order-confirmed")
    public CartCleanupResponse orderConfirmed(@RequestHeader("X-Internal-Api-Key") String key,
                                              @Valid @RequestBody OrderConfirmedRequest request) {
        guard.verify(key); return cleanup.orderConfirmed(request);
    }
}
