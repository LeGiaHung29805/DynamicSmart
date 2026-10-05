package com.dynamicmart.cart_service.controller;

import com.dynamicmart.cart_service.client.IdentityManagementClient;
import tools.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerAccountController {
    private final IdentityManagementClient identity;
    public CustomerAccountController(IdentityManagementClient identity) { this.identity = identity; }

    @GetMapping("/api/v1/profile") public JsonNode profile(@AuthenticationPrincipal Jwt jwt) { return identity.profile(jwt.getTokenValue()); }
    @PutMapping("/api/v1/profile") public JsonNode updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody JsonNode body) { return identity.updateProfile(jwt.getTokenValue(), body); }
    @GetMapping("/api/v1/addresses") public JsonNode addresses(@AuthenticationPrincipal Jwt jwt) { return identity.addresses(jwt.getTokenValue()); }
    @PostMapping("/api/v1/addresses") @ResponseStatus(HttpStatus.CREATED)
    public JsonNode createAddress(@AuthenticationPrincipal Jwt jwt, @RequestBody JsonNode body) { return identity.createAddress(jwt.getTokenValue(), body); }
    @PutMapping("/api/v1/addresses/{id}") public JsonNode updateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody JsonNode body) { return identity.updateAddress(jwt.getTokenValue(), id, body); }
    @PostMapping("/api/v1/addresses/{id}/default") public JsonNode makeDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return identity.makeDefault(jwt.getTokenValue(), id); }
    @DeleteMapping("/api/v1/addresses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { identity.deactivateAddress(jwt.getTokenValue(), id); }

    @GetMapping("/api/v1/admin/users")
    public JsonNode users(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String query,
                          @RequestParam(required = false) String status, @RequestParam(required = false) String role,
                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return identity.users(jwt.getTokenValue(), query, status, role, page, size);
    }
    @GetMapping("/api/v1/admin/users/{id}") public JsonNode user(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return identity.user(jwt.getTokenValue(), id); }
    @PatchMapping("/api/v1/admin/users/{id}")
    public JsonNode manageUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @RequestHeader("Idempotency-Key") UUID key, @RequestBody JsonNode body) {
        return identity.manageUser(jwt.getTokenValue(), id, key, body);
    }
}
