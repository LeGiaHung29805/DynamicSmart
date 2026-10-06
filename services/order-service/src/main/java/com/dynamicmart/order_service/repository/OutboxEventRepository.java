package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
            select event.*
            from outbox_events event
            where (event.status = 'PENDING' and event.available_at <= :now)
               or (event.status = 'PROCESSING'
                   and (event.processing_lease_until is null or event.processing_lease_until <= :now))
            order by event.created_at, event.id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<OutboxEvent> findNextPublishCandidateForUpdate(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEvent event where event.id = :id")
    Optional<OutboxEvent> findByIdForUpdate(@Param("id") UUID id);
}
