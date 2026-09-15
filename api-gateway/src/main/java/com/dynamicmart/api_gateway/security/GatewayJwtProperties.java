package com.dynamicmart.api_gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record GatewayJwtProperties(String issuer, String hmacSecretBase64) { }
