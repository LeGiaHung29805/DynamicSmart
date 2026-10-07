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
@Table(name = "product_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductAnswer {
    @Id
    private UUID id;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;
    @Column(name = "admin_id", nullable = false)
    private UUID adminId;
    @Column(nullable = false)
    private String content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductAnswer(UUID questionId, UUID adminId, String content, Instant now) {
        this.id = UUID.randomUUID();
        this.questionId = questionId;
        this.adminId = adminId;
        this.content = content;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
