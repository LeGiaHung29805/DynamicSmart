package com.dynamicmart.payment_service.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtAuthenticationConverter converter,
                                 InternalApiAuthenticationFilter internalApiFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/payments/methods",
                                "/api/v1/locations/provinces",
                                "/api/v1/locations/wards").permitAll()
                        .requestMatchers("/api/v1/payments/vnpay/ipn",
                                "/api/v1/payments/zalopay/callback",
                                "/api/v1/payments/payos/webhook",
                                "/api/v1/payments/sepay/webhook").permitAll()
                        .requestMatchers("/api/v1/payments/my-orders/**",
                                "/api/v1/payments/vnpay/return-status",
                                "/api/v1/payments/return-status").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/locations/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/payments",
                                "/api/v1/payments/admin/*",
                                "/api/v1/payments/*/callback-audits",
                                "/api/v1/payments/*/attempts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/payments/order-context",
                                "/api/v1/payments/*/vnpay-attempt",
                                "/api/v1/payments/*/attempts",
                                "/api/v1/payments/orders/*/cod-collected",
                                "/api/v1/shipping/quotes",
                                "/api/v1/shipping/quotes/*/validate").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/locations/validate",
                                "/api/v1/payments/orders/*",
                                "/api/v1/payments/*").hasRole("SERVICE")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .addFilterBefore(internalApiFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt.issuer}") String issuer,
                          @Value("${app.security.jwt.hmac-secret-base64}") String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 không phải Base64 hợp lệ.", exception);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 phải giải mã được ít nhất 32 bytes.");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::toAuthorities);
        return converter;
    }

    private java.util.Collection<org.springframework.security.core.GrantedAuthority> toAuthorities(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) {
            return java.util.List.of();
        }
        return java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }
}
