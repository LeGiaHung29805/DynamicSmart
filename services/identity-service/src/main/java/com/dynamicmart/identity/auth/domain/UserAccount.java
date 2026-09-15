package com.dynamicmart.identity.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "email_normalized", nullable = false, length = 254, unique = true)
    private String emailNormalized;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(length = 20)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
    @Column(name = "auth_version", nullable = false)
    private int authVersion;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() { }

    public UserAccount(UUID id, String email, String emailNormalized, String passwordHash, String fullName, Instant now) {
        this.id = id; this.email = email; this.emailNormalized = emailNormalized; this.passwordHash = passwordHash;
        this.fullName = fullName; this.role = UserRole.CUSTOMER; this.status = UserStatus.ACTIVE;
        this.authVersion = 0; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public int getAuthVersion() { return authVersion; }
    public void markLoggedIn(Instant now) { this.lastLoginAt = now; this.updatedAt = now; }
}
