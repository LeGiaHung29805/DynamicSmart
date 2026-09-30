package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.entity.OutboxEvent;
import com.dynamicmart.catalog_service.repository.OutboxEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class CatalogOutboxService {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CatalogOutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String aggregateType, UUID aggregateId, String eventType,
                       Object payload, UUID correlationId) {
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId,
                eventType, objectMapper.valueToTree(payload), correlationId));
    }
}
