package com.dynamicmart.identity.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "requested_ip_hash", length = 64)
    private String requestedIpHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordResetToken() { }

    public PasswordResetToken(UUID id, UserAccount user, String tokenHash, Instant expiresAt,
                              String requestedIpHash, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.requestedIpHash = requestedIpHash;
        this.createdAt = createdAt;
    }

    public UserAccount getUser() { return user; }
    public boolean isUsable(Instant now) { return usedAt == null && expiresAt.isAfter(now); }
    public void markUsed(Instant now) { this.usedAt = now; }
}
