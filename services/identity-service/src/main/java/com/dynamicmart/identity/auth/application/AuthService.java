package com.dynamicmart.identity.auth.application;

import com.dynamicmart.identity.auth.api.AuthResponse;
import com.dynamicmart.identity.auth.api.LoginRequest;
import com.dynamicmart.identity.auth.api.RegisterRequest;
import com.dynamicmart.identity.auth.domain.PasswordResetToken;
import com.dynamicmart.identity.auth.domain.PasswordResetTokenRepository;
import com.dynamicmart.identity.auth.config.JwtProperties;
import com.dynamicmart.identity.auth.domain.RefreshToken;
import com.dynamicmart.identity.auth.domain.RefreshTokenRepository;
import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import com.dynamicmart.identity.auth.domain.UserStatus;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordResetDelivery passwordResetDelivery;

    public AuthService(UserAccountRepository users, RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository passwordResetTokens, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService, JwtProperties jwtProperties,
                       PasswordResetDelivery passwordResetDelivery) {
        this.users = users; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder;
        this.passwordResetTokens = passwordResetTokens; this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties; this.passwordResetDelivery = passwordResetDelivery;
    }

    @Transactional
    public AuthenticationResult register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (users.existsByEmailNormalized(normalizedEmail)) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email này đã được sử dụng.");
        }
        Instant now = Instant.now();
        UserAccount user = new UserAccount(UUID.randomUUID(), request.email().trim(), normalizedEmail,
                passwordEncoder.encode(request.password()), request.fullName().trim(), now);
        users.save(user);
        return authenticate(user, now, UUID.randomUUID());
    }

    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        UserAccount user = users.findByEmailNormalized(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw invalidCredentials();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản hiện không thể đăng nhập.");
        }
        Instant now = Instant.now();
        user.markLoggedIn(now);
        return authenticate(user, now, UUID.randomUUID());
    }

    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        Instant now = Instant.now();
        RefreshToken current = refreshTokens.findByTokenHash(TokenHashing.sha256(rawRefreshToken))
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Phiên đăng nhập không hợp lệ."));
        if (!current.isUsable(now) || current.getUser().getStatus() != UserStatus.ACTIVE) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Phiên đăng nhập đã hết hạn.");
        }
        AuthenticationResult next = authenticate(current.getUser(), now, current.getFamilyId());
        current.replaceBy(next.refreshTokenId(), now);
        return next;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokens.findByTokenHash(TokenHashing.sha256(rawRefreshToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional
    public void requestPasswordReset(String email, String requesterIp) {
        users.findByEmailNormalized(normalizeEmail(email)).ifPresent(user -> {
            Instant now = Instant.now();
            passwordResetTokens.invalidateUnusedByUserId(user.getId(), now);
            String rawToken = randomToken();
            String ipHash = requesterIp == null || requesterIp.isBlank() ? null : TokenHashing.sha256(requesterIp);
            passwordResetTokens.save(new PasswordResetToken(UUID.randomUUID(), user, TokenHashing.sha256(rawToken),
                    now.plusSeconds(900), ipHash, now));
            passwordResetDelivery.deliver(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken token = passwordResetTokens.findByTokenHash(TokenHashing.sha256(rawToken))
                .orElseThrow(this::invalidResetToken);
        if (!token.isUsable(now) || token.getUser().getStatus() != UserStatus.ACTIVE) throw invalidResetToken();
        token.markUsed(now);
        token.getUser().resetPassword(passwordEncoder.encode(newPassword), now);
        refreshTokens.revokeAllByUserId(token.getUser().getId(), now);
        passwordResetTokens.invalidateUnusedByUserId(token.getUser().getId(), now);
    }

    private AuthenticationResult authenticate(UserAccount user, Instant now, UUID familyId) {
        String rawRefreshToken = randomToken();
        UUID refreshId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(refreshId, user, TokenHashing.sha256(rawRefreshToken), familyId,
                now.plusSeconds(jwtProperties.refreshTokenTtlDays() * 86_400L), now);
        refreshTokens.save(refreshToken);
        JwtTokenService.IssuedAccessToken accessToken = jwtTokenService.issue(user, now);
        AuthResponse response = new AuthResponse(accessToken.value(), accessToken.expiresAt(),
                new AuthResponse.UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name()));
        return new AuthenticationResult(response, rawRefreshToken, refreshId);
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
    }
    private AuthException invalidResetToken() {
        return new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID",
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
    }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String randomToken() {
        byte[] bytes = new byte[48]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public record AuthenticationResult(AuthResponse response, String rawRefreshToken, UUID refreshTokenId) { }
}
