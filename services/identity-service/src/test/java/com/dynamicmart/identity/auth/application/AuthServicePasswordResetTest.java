package com.dynamicmart.identity.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.identity.auth.config.JwtProperties;
import com.dynamicmart.identity.auth.domain.PasswordResetToken;
import com.dynamicmart.identity.auth.domain.PasswordResetTokenRepository;
import com.dynamicmart.identity.auth.domain.RefreshTokenRepository;
import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.auth.domain.UserAccountRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServicePasswordResetTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final PasswordResetDelivery delivery = mock(PasswordResetDelivery.class);
    private final AuthService service = new AuthService(users, refreshTokens, resetTokens, encoder,
            mock(JwtTokenService.class), new JwtProperties("issuer", "secret", 900, 30), delivery);

    @Test
    void forgotPasswordCreatesHashedSingleUseTokenAndDeliversRawToken() {
        UserAccount user = user();
        when(users.findByEmailNormalized("user@example.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset(" USER@example.com ", "127.0.0.1");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(resetTokens).invalidateUnusedByUserId(eq(user.getId()), any());
        verify(resetTokens).save(tokenCaptor.capture());
        verify(delivery).deliver(eq(user), rawTokenCaptor.capture());
        assertThat(rawTokenCaptor.getValue()).isNotBlank();
        assertThat(tokenCaptor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() {
        when(users.findByEmailNormalized("missing@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("missing@example.com", "127.0.0.1");

        verify(resetTokens, never()).save(any());
        verify(delivery, never()).deliver(any(), any());
    }

    @Test
    void resetChangesPasswordInvalidatesSessionsAndCannotReuseToken() {
        UserAccount user = user();
        String rawToken = "valid-reset-token";
        PasswordResetToken token = new PasswordResetToken(UUID.randomUUID(), user, TokenHashing.sha256(rawToken),
                Instant.now().plusSeconds(600), null, Instant.now());
        when(resetTokens.findByTokenHash(TokenHashing.sha256(rawToken))).thenReturn(Optional.of(token));
        when(encoder.encode("new-password")).thenReturn("new-password-hash");

        service.resetPassword(rawToken, "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");
        assertThat(user.getAuthVersion()).isEqualTo(1);
        verify(refreshTokens).revokeAllByUserId(eq(user.getId()), any());
        assertThatThrownBy(() -> service.resetPassword(rawToken, "another-password"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("không hợp lệ");
    }

    private UserAccount user() {
        return new UserAccount(UUID.randomUUID(), "user@example.com", "user@example.com", "old-hash",
                "User", Instant.now());
    }
}
