package com.dynamicmart.identity.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String issuer, String hmacSecretBase64, long accessTokenTtlSeconds, long refreshTokenTtlDays) { }
