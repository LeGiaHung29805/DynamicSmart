package com.dynamicmart.engagement_service.entity;

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
@Table(name = "chat_conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversation {
    @Id
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "last_message_at")
    private Instant lastMessageAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ChatConversation(UUID customerId, Instant now) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.status = "OPEN";
        this.lastMessageAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void assign(UUID adminId, Instant now) {
        this.assignedAdminId = adminId;
        this.updatedAt = now;
    }

    public void touchMessage(Instant now) {
        this.lastMessageAt = now;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        this.status = "CLOSED";
        this.updatedAt = now;
    }
}
