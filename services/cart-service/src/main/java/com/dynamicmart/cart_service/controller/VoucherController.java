package com.dynamicmart.cart_service.controller;

import static com.dynamicmart.cart_service.dto.VoucherDtos.*;
import static com.dynamicmart.cart_service.dto.InternalCartDtos.ReservationResponse;
import static com.dynamicmart.cart_service.dto.PromotionDtos.PromotionAuditResponse;

import com.dynamicmart.cart_service.service.VoucherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class VoucherController {
    private final VoucherService vouchers;
    public VoucherController(VoucherService vouchers) { this.vouchers = vouchers; }

    @GetMapping("/vouchers/wallet")
    public List<VoucherResponse> wallet(@AuthenticationPrincipal Jwt jwt) { return vouchers.wallet(user(jwt)); }
    @PostMapping("/vouchers/preview")
    public VoucherResponse preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VoucherPreviewRequest request) {
        return vouchers.preview(user(jwt), request);
    }
    @GetMapping("/admin/vouchers") public List<VoucherResponse> list() { return vouchers.adminList(); }
    @GetMapping("/admin/vouchers/{id}/audits") public List<PromotionAuditResponse> audits(@PathVariable UUID id) { return vouchers.audits(id); }
    @GetMapping("/admin/voucher-reservations") public List<ReservationResponse> reservations() { return vouchers.reservationHistory(); }
    @PostMapping("/admin/vouchers")
    public VoucherResponse create(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") UUID key,
                                  @Valid @RequestBody VoucherRuleRequest request) {
        return vouchers.create(user(jwt), key, request);
    }
    @PutMapping("/admin/vouchers/{id}")
    public VoucherResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @RequestHeader("Idempotency-Key") UUID key,
                                  @Valid @RequestBody VoucherRuleRequest request) {
        return vouchers.update(user(jwt), id, key, request);
    }
    @PatchMapping("/admin/vouchers/{id}/status")
    public VoucherResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @RequestHeader("Idempotency-Key") UUID key,
                                  @Valid @RequestBody VoucherStatusRequest request) {
        return vouchers.changeStatus(user(jwt), id, key, request);
    }
    @PostMapping("/admin/vouchers/{id}/assignments")
    public AssignmentResponse assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestHeader("Idempotency-Key") UUID key,
                                     @Valid @RequestBody AssignmentRequest request) {
        return vouchers.assign(user(jwt), id, key, request);
    }
    @DeleteMapping("/admin/voucher-assignments/{assignmentId}")
    public AssignmentResponse revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID assignmentId,
                                     @RequestHeader("Idempotency-Key") UUID key,
                                     @RequestParam String reason) {
        return vouchers.revoke(user(jwt), assignmentId, key, reason);
    }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
