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
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserAccount user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "replaced_by_id") private UUID replacedById;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected RefreshToken() { }
    public RefreshToken(UUID id, UserAccount user, String tokenHash, UUID familyId, Instant expiresAt, Instant now) {
        this.id = id; this.user = user; this.tokenHash = tokenHash; this.familyId = familyId; this.expiresAt = expiresAt; this.createdAt = now;
    }
    public boolean isUsable(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void replaceBy(UUID nextId, Instant now) { this.replacedById = nextId; this.revokedAt = now; }
    public void revoke(Instant now) { this.revokedAt = now; }
    public UserAccount getUser() { return user; }
    public UUID getFamilyId() { return familyId; }
}
