package com.dynamicmart.engagement_service.controller;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

final class CurrentUser {
    private CurrentUser() {
    }

    static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
