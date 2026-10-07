package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.entity.OutboxEvent;
import com.dynamicmart.catalog_service.repository.OutboxEventRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class CatalogOutboxService {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public CatalogOutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper,
                                @Value("${app.outbox.enabled:true}") boolean enabled) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public void record(String aggregateType, UUID aggregateId, String eventType,
                       Object payload, UUID correlationId) {
        if (!enabled) return;
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId,
                eventType, objectMapper.valueToTree(payload), correlationId));
    }
}
