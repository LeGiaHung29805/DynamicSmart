package com.dynamicmart.order_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRoleConverterTests {
    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void convertsSupportedCustomerRole() {
        var authorities = converter.convert(jwtWithRole("CUSTOMER"));
        assertEquals("ROLE_CUSTOMER", authorities.iterator().next().getAuthority());
    }

    @Test
    void convertsSupportedAdminRole() {
        var authorities = converter.convert(jwtWithRole("ADMIN"));
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void rejectsUnknownRole() {
        assertTrue(converter.convert(jwtWithRole("SELLER")).isEmpty());
    }

    private Jwt jwtWithRole(String role) {
        Instant now = Instant.parse("2026-09-23T00:00:00Z");
        return new Jwt("token", now, now.plusSeconds(900), Map.of("alg", "HS256"),
                Map.of("sub", "00000000-0000-0000-0000-000000000001", "role", role));
    }
}
