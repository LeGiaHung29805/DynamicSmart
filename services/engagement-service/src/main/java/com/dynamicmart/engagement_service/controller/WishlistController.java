package com.dynamicmart.engagement_service.controller;

import com.dynamicmart.engagement_service.dto.request.AddWishlistItemRequest;
import com.dynamicmart.engagement_service.dto.response.ApiResponse;
import com.dynamicmart.engagement_service.dto.response.WishlistResponse;
import com.dynamicmart.engagement_service.service.WishlistService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlists")
public class WishlistController {
    private final WishlistService wishlists;

    public WishlistController(WishlistService wishlists) {
        this.wishlists = wishlists;
    }

    @GetMapping("/me")
    public ApiResponse<WishlistResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.of(wishlists.get(CurrentUser.id(jwt)));
    }

    @PostMapping("/me/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishlistResponse> add(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody AddWishlistItemRequest request) {
        return ApiResponse.of(wishlists.add(CurrentUser.id(jwt), request));
    }

    @DeleteMapping("/me/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        wishlists.remove(CurrentUser.id(jwt), productId);
    }
}
