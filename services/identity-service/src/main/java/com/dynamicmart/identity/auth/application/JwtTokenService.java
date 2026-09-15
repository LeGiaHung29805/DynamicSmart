package com.dynamicmart.identity.auth.application;

import com.dynamicmart.identity.auth.config.JwtProperties;
import com.dynamicmart.identity.auth.domain.UserAccount;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedAccessToken issue(UserAccount user, Instant now) {
        Instant expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer()).subject(user.getId().toString())
                .issuedAt(now).expiresAt(expiresAt).id(java.util.UUID.randomUUID().toString())
                .claim("role", user.getRole().name()).claim("authVersion", user.getAuthVersion())
                .build();
        String value = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }

    public record IssuedAccessToken(String value, Instant expiresAt) { }
}
