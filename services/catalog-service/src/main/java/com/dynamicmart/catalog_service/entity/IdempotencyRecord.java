package com.dynamicmart.catalog_service.entity;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {
    @EmbeddedId private IdempotencyRecordId id;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "status_code", nullable = false) private int statusCode;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb") private JsonNode responseBody;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public IdempotencyRecord(IdempotencyRecordId id, String requestHash, int statusCode,
                             JsonNode responseBody, Instant expiresAt) {
        this.id = id;
        this.requestHash = requestHash;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
