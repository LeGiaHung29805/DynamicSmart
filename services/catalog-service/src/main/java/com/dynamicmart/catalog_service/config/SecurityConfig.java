package com.dynamicmart.catalog_service.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/catalog/categories/**",
                                "/api/v1/catalog/products/**",
                                "/api/v1/catalog/variants/**",
                                "/api/v1/catalog/media/**").permitAll()
                        .requestMatchers("/api/v1/catalog/internal/**").permitAll()
                        .requestMatchers("/api/v1/catalog/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(CatalogJwtProperties properties) {
        if (properties.hmacSecretBase64() == null || properties.hmacSecretBase64().isBlank()) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 chưa được cấu hình cho catalog-service.");
        }
        byte[] bytes = Base64.getDecoder().decode(properties.hmacSecretBase64());
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 phải giải mã được ít nhất 32 bytes.");
        }
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
        return java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }
}
