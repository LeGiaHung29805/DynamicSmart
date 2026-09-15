package com.dynamicmart.identity.auth.api;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(String accessToken, Instant accessTokenExpiresAt, UserResponse user) {
    public record UserResponse(UUID id, String email, String fullName, String role) { }
}
