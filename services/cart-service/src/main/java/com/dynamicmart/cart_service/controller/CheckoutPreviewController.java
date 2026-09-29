package com.dynamicmart.cart_service.controller;

import static com.dynamicmart.cart_service.dto.CheckoutPreviewDtos.*;

import com.dynamicmart.cart_service.service.CheckoutPreviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart/checkout")
public class CheckoutPreviewController {
    private final CheckoutPreviewService checkout;
    public CheckoutPreviewController(CheckoutPreviewService checkout) { this.checkout = checkout; }
    @PostMapping("/preview")
    public PreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreviewRequest request) {
        return checkout.preview(UUID.fromString(jwt.getSubject()), request);
    }
}
