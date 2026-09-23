package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findAllByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status, Instant now, Pageable pageable);
}
