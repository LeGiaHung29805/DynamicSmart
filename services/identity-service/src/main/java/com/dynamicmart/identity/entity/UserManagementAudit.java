package com.dynamicmart.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_management_audits")
public class UserManagementAudit {
    @Id private UUID id;
    @Column(name = "target_user_id", nullable = false) private UUID targetUserId;
    @Column(name = "actor_admin_id", nullable = false) private UUID actorAdminId;
    @Column(nullable = false, length = 30) private String action;
    @Column(name = "old_role", length = 20) private String oldRole;
    @Column(name = "new_role", length = 20) private String newRole;
    @Column(name = "old_status", length = 20) private String oldStatus;
    @Column(name = "new_status", length = 20) private String newStatus;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected UserManagementAudit() { }
    public UserManagementAudit(UUID targetUserId, UUID actorAdminId, String action, String oldRole, String newRole,
                               String oldStatus, String newStatus, String reason, UUID idempotencyKey, Instant now) {
        this.id = UUID.randomUUID(); this.targetUserId = targetUserId; this.actorAdminId = actorAdminId;
        this.action = action; this.oldRole = oldRole; this.newRole = newRole; this.oldStatus = oldStatus;
        this.newStatus = newStatus; this.reason = reason; this.idempotencyKey = idempotencyKey; this.createdAt = now;
    }
    public UUID getId() { return id; }
    public String getAction() { return action; }
    public String getOldRole() { return oldRole; }
    public String getNewRole() { return newRole; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
