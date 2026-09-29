package com.dynamicmart.order_service.config;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter authenticationConverter,
            SecurityErrorWriter errorWriter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/orders/**", "/api/v1/checkout/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.unauthorized(request, response))
                        .accessDeniedHandler((request, response, exception) -> errorWriter.forbidden(request, response)))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, exception) -> errorWriter.unauthorized(request, response))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("JWT issuer chưa được cấu hình.");
        }
        if (properties.hmacSecretBase64() == null || properties.hmacSecretBase64().isBlank()) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 chưa được cấu hình.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(properties.hmacSecretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 không phải Base64 hợp lệ.", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 phải giải mã được ít nhất 32 bytes.");
        }

        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
