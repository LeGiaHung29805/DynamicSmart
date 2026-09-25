package com.dynamicmart.cart_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void roleComesOnlyFromSignedRoleClaim() {
        Jwt customer = jwt("CUSTOMER"); Jwt admin = jwt("ADMIN"); Jwt forged = jwt("SUPER_ADMIN");
        assertThat(config.jwtAuthenticationConverter().convert(customer).getAuthorities()).extracting("authority").contains("ROLE_CUSTOMER").doesNotContain("ROLE_ADMIN");
        assertThat(config.jwtAuthenticationConverter().convert(admin).getAuthorities()).extracting("authority").contains("ROLE_ADMIN").doesNotContain("ROLE_CUSTOMER");
        assertThat(config.jwtAuthenticationConverter().convert(forged).getAuthorities()).extracting("authority").doesNotContain("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    private Jwt jwt(String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test").header("alg", "HS256").subject("00000000-0000-0000-0000-000000000001")
                .issuedAt(now).expiresAt(now.plusSeconds(60)).claim("role", role).build();
    }
}
