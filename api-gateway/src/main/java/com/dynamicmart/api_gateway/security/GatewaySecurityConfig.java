package com.dynamicmart.api_gateway.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewaySecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter,
                                            SessionValidationFilter sessionValidationFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/internal/**").denyAll()
                        .requestMatchers("/actuator/health", "/api/v1/auth/**",
                                "/api/v1/payments/vnpay/ipn",
                                "/api/v1/payments/zalopay/callback",
                                "/api/v1/payments/payos/webhook",
                                "/api/v1/payments/sepay/webhook",
                                "/api/v1/assistant/chat",
                                "/api/v1/cart/promotions/prices/**").permitAll()
                        // Dịch vụ đích xác thực lại các route nội bộ bằng X-Internal-Api-Key.
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/payments/order-context",
                                "/api/v1/payments/*/vnpay-attempt",
                                "/api/v1/payments/*/attempts",
                                "/api/v1/shipping/quotes",
                                "/api/v1/shipping/quotes/*/validate").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/locations/validate",
                                "/api/v1/payments/orders/*").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/locations/provinces",
                                "/api/v1/locations/wards",
                                "/api/v1/catalog/products/**",
                                "/api/v1/catalog/categories/**",
                                "/api/v1/catalog/variants/**",
                                "/api/v1/reviews/products/**",
                                "/api/v1/product-questions/products/**",
                                "/api/v1/reports/products/best-sellers").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/engagement/internal/events/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/locations/sync").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments", "/api/v1/payments/*/callback-audits", "/api/v1/payments/*/attempts").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/users/**", "/api/v1/cart/admin/**",
                                "/api/v1/admin/reviews/**", "/api/v1/admin/product-questions/**",
                                "/api/v1/admin/support/conversations/**", "/api/v1/reports/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .addFilterAfter(sessionValidationFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(GatewayJwtProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.hmacSecretBase64());
        if (bytes.length < 32) throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 phải giải mã được ít nhất 32 bytes.");
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
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
        if (!"CUSTOMER".equals(role) && !"ADMIN".equals(role)) return java.util.List.of();
        return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.frontend-origin}") String frontendOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of(frontendOrigin));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-Id"));
        config.setExposedHeaders(java.util.List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
