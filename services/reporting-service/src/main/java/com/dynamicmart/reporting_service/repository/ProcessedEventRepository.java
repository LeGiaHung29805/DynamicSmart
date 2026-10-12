package com.dynamicmart.reporting_service.repository;

import com.dynamicmart.reporting_service.entity.ProcessedEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, event_type, producer, processed_at, correlation_id)
            VALUES (?1, ?2, ?3, ?4, ?5)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID eventId, String eventType, String producer,
                       Instant processedAt, UUID correlationId);
}
