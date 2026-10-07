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
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {
    @Id
    private UUID id;
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;
    @Column(name = "sender_id", nullable = false)
    private UUID senderId;
    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole;
    @Column(nullable = false)
    private String content;
    @Column(name = "idempotency_key")
    private UUID idempotencyKey;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ChatMessage(UUID conversationId, UUID senderId, String senderRole, String content,
                       UUID idempotencyKey, Instant now) {
        this.id = UUID.randomUUID();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
    }
}
