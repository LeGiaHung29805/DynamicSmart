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
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 50) private String type;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false) private String content;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public Notification(UUID customerId, String type, String title, String content, Instant now) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.createdAt = now;
    }

    public void markRead(Instant now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }
}
