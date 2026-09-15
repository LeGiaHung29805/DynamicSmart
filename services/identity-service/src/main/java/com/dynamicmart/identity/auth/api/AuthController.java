package com.dynamicmart.identity.auth.api;

import com.dynamicmart.identity.auth.application.AuthService;
import com.dynamicmart.identity.auth.application.AuthException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "dm_refresh_token";
    private final AuthService authService;
    private final long refreshTokenTtlDays;
    private final boolean refreshCookieSecure;

    public AuthController(AuthService authService,
                          @Value("${app.security.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays,
                          @Value("${app.security.refresh-cookie-secure:false}") boolean refreshCookieSecure) {
        this.authService = authService;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return respond(authService.register(request), response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respond(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            clearRefreshCookie(response);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISSING", "Phiên đăng nhập không tồn tại.");
        }
        return respond(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> respond(AuthService.AuthenticationResult result, HttpServletResponse response) {
        addRefreshCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(result.response());
    }

    private void addRefreshCookie(HttpServletResponse response, String value) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ofDays(refreshTokenTtlDays)).build().toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Strict").path("/api/v1/auth")
                .maxAge(Duration.ZERO).build().toString());
    }
}
