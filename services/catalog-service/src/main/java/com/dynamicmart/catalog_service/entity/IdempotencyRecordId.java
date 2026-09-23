package com.dynamicmart.catalog_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record IdempotencyRecordId(
        @Column(name = "operation", length = 100) String operation,
        @Column(name = "idempotency_key") UUID idempotencyKey
) implements Serializable {
}
