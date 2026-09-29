package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.IdempotencyRecord;
import com.dynamicmart.order_service.entity.IdempotencyRecordId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, IdempotencyRecordId> {
    Optional<IdempotencyRecord> findByOperationAndIdempotencyKeyAndExpiresAtAfter(
            String operation, UUID idempotencyKey, Instant now);
}
