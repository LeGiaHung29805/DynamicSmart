package com.dynamicmart.cart_service.controller;

import static com.dynamicmart.cart_service.dto.PromotionDtos.*;

import com.dynamicmart.cart_service.service.DirectSaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
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

@RestController @Validated
public class PromotionController {
    private final DirectSaleService directSales;
    public PromotionController(DirectSaleService directSales) { this.directSales = directSales; }

    @GetMapping("/api/v1/cart/promotions/prices/{variantId}")
    public DirectSalePriceResponse price(@PathVariable UUID variantId, @RequestParam @PositiveOrZero long listPriceVnd) {
        return directSales.resolve(variantId, listPriceVnd);
    }

    @GetMapping("/api/v1/cart/admin/direct-sales")
    public List<DirectSaleResponse> list() { return directSales.list(); }
    @GetMapping("/api/v1/cart/admin/direct-sales/{id}")
    public DirectSaleResponse get(@PathVariable UUID id) { return directSales.get(id); }
    @GetMapping("/api/v1/cart/admin/direct-sales/{id}/audits")
    public List<PromotionAuditResponse> audits(@PathVariable UUID id) { return directSales.audits(id); }
    @PostMapping("/api/v1/cart/admin/direct-sales")
    public DirectSaleResponse create(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") UUID key,
                                     @Valid @RequestBody DirectSaleRequest request) {
        return directSales.create(user(jwt), key, request);
    }
    @PutMapping("/api/v1/cart/admin/direct-sales/{id}")
    public DirectSaleResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestHeader("Idempotency-Key") UUID key,
                                     @Valid @RequestBody DirectSaleRequest request) {
        return directSales.update(user(jwt), id, key, request);
    }
    @PatchMapping("/api/v1/cart/admin/direct-sales/{id}/status")
    public DirectSaleResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestHeader("Idempotency-Key") UUID key,
                                     @Valid @RequestBody StatusRequest request) {
        return directSales.changeStatus(user(jwt), id, key, request);
    }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
