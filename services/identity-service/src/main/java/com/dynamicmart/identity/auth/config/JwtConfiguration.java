package com.dynamicmart.identity.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfiguration {
    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.hmacSecretBase64());
        if (bytes.length < 32) throw new IllegalStateException("JWT_HMAC_SECRET_BASE64 phải giải mã được ít nhất 32 bytes.");
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
    }
}
