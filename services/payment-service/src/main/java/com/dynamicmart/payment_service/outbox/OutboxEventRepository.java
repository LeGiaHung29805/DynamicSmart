package com.dynamicmart.payment_service.outbox;

import com.dynamicmart.payment_service.outbox.OutboxEvent;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusAndAvailableAtBeforeOrderByCreatedAtAsc(String status, Instant now);
}
