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
@Table(name = "review_images")
@Getter @Setter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {
    @Id private UUID id;
    @Column(name = "review_id", nullable = false) private UUID reviewId;
    @Column(name = "image_url", nullable = false, length = 1000) private String imageUrl;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
