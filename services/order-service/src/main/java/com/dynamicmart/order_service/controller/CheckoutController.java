package com.dynamicmart.order_service.controller;

import com.dynamicmart.order_service.config.CurrentCustomer;
import com.dynamicmart.order_service.dto.request.CheckoutPreviewRequest;
import com.dynamicmart.order_service.dto.request.CreateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.request.UpdateCheckoutSessionRequest;
import com.dynamicmart.order_service.dto.response.CheckoutPreviewResponse;
import com.dynamicmart.order_service.dto.response.CheckoutSessionResponse;
import com.dynamicmart.order_service.service.CheckoutPreviewService;
import com.dynamicmart.order_service.service.CheckoutSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/sessions")
public class CheckoutController {
    private final CheckoutSessionService checkoutSessions;
    private final CheckoutPreviewService checkoutPreviews;
    private final CurrentCustomer currentCustomer;

    public CheckoutController(
            CheckoutSessionService checkoutSessions,
            CheckoutPreviewService checkoutPreviews,
            CurrentCustomer currentCustomer) {
        this.checkoutSessions = checkoutSessions;
        this.checkoutPreviews = checkoutPreviews;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping
    public ResponseEntity<CheckoutSessionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutSessions.create(currentCustomer.idFrom(jwt), request));
    }

    @GetMapping("/{sessionId}")
    public CheckoutSessionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        return checkoutSessions.get(currentCustomer.idFrom(jwt), sessionId);
    }

    @PatchMapping("/{sessionId}")
    public CheckoutSessionResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @RequestBody UpdateCheckoutSessionRequest request) {
        return checkoutSessions.update(currentCustomer.idFrom(jwt), sessionId, request);
    }

    @PostMapping("/{sessionId}/preview")
    public CheckoutPreviewResponse preview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        return checkoutPreviews.preview(currentCustomer.idFrom(jwt), sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    public CheckoutSessionResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        return checkoutSessions.cancel(currentCustomer.idFrom(jwt), sessionId);
    }
}
