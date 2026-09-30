package com.dynamicmart.cart_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "customer_vouchers")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerVoucher {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "voucher_id", nullable = false) private UUID voucherId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(nullable = false, length = 30) private String source;
    @Column(name = "assigned_by") private UUID assignedBy;

    public CustomerVoucher(UUID customerId, UUID voucherId, Instant expiresAt, UUID assignedBy, Instant now) {
        this.id = UUID.randomUUID(); this.customerId = customerId; this.voucherId = voucherId;
        this.status = "AVAILABLE"; this.assignedAt = now; this.expiresAt = expiresAt;
        this.source = "ADMIN_ASSIGNMENT"; this.assignedBy = assignedBy;
    }
    public boolean availableAt(Instant now) { return "AVAILABLE".equals(status) && (expiresAt == null || now.isBefore(expiresAt)); }
    public void revoke() { this.status = "REVOKED"; }
    public void useUp() { this.status = "USED_UP"; }
    public void makeAvailable(Instant expiresAt, UUID assignedBy, Instant now) {
        this.status = "AVAILABLE"; this.expiresAt = expiresAt; this.assignedBy = assignedBy; this.assignedAt = now;
    }
}
