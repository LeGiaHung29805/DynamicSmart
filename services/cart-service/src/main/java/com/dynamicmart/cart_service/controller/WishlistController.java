package com.dynamicmart.cart_service.controller;

import com.dynamicmart.cart_service.dto.response.WishlistResponse;
import com.dynamicmart.cart_service.service.WishlistService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {
    private final WishlistService wishlist;
    public WishlistController(WishlistService wishlist) { this.wishlist = wishlist; }
    @GetMapping public WishlistResponse get(@AuthenticationPrincipal Jwt jwt) { return wishlist.get(userId(jwt)); }
    @PutMapping("/items/{productId}") public WishlistResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) { return wishlist.add(userId(jwt), productId); }
    @DeleteMapping("/items/{productId}") public WishlistResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) { return wishlist.remove(userId(jwt), productId); }
    private UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
