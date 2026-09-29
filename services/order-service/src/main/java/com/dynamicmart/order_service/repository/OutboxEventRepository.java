package com.dynamicmart.order_service.repository;
import com.dynamicmart.order_service.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByStatusAndAvailableAtBeforeOrderByCreatedAtAsc(String status, Instant availableAt);
}
