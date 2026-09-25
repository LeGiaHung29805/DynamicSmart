package com.dynamicmart.cart_service.controller;

import com.dynamicmart.cart_service.dto.request.AddCartItemRequest;
import com.dynamicmart.cart_service.dto.request.UpdateCartItemRequest;
import com.dynamicmart.cart_service.dto.response.CartResponse;
import com.dynamicmart.cart_service.service.CartApplicationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartApplicationService carts;
    public CartController(CartApplicationService carts) { this.carts = carts; }
    @GetMapping public CartResponse get(@AuthenticationPrincipal Jwt jwt) { return carts.get(user(jwt)); }
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED)
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddCartItemRequest request) { return carts.add(user(jwt), request); }
    @PatchMapping("/items/{itemId}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemRequest request) { return carts.update(user(jwt), itemId, request); }
    @DeleteMapping("/items/{itemId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId) { carts.remove(user(jwt), itemId); }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
