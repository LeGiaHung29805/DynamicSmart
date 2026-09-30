package com.dynamicmart.api_gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionValidationFilter extends OncePerRequestFilter {
    private final RestClient identity; private final String internalKey;
    public SessionValidationFilter(RestClient.Builder builder,
                                   @Value("${app.routes.identity-service-url}") String identityUrl,
                                   @Value("${app.security.internal-api-key}") String internalKey) {
        this.identity = builder.baseUrl(identityUrl).build(); this.internalKey = internalKey;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") || path.equals("/actuator/health") ||
                path.equals("/api/v1/payments/vnpay/ipn") || path.startsWith("/api/v1/cart/promotions/prices/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) { chain.doFilter(request, response); return; }
        Number authVersionClaim = token.getToken().getClaim("authVersion");
        int authVersion = authVersionClaim == null ? -1 : authVersionClaim.intValue();
        boolean valid = false;
        try {
            SessionValidation result = identity.get().uri(uri -> uri.path("/api/v1/auth/internal/session-validations")
                            .queryParam("userId", UUID.fromString(token.getToken().getSubject()))
                            .queryParam("authVersion", authVersion).build())
                    .header("X-Internal-Api-Key", internalKey).retrieve().body(SessionValidation.class);
            valid = result != null && result.valid();
        } catch (RestClientException | IllegalArgumentException ignored) { valid = false; }
        if (!valid) {
            SecurityContextHolder.clearContext(); response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"SESSION_INVALID\",\"message\":\"Phiên đăng nhập đã hết hiệu lực.\"}"); return;
        }
        chain.doFilter(request, response);
    }
    private record SessionValidation(boolean valid) { }
}
