package com.dynamicmart.catalog_service.service;

import com.dynamicmart.catalog_service.dto.response.CatalogEventMessage;
import com.dynamicmart.catalog_service.entity.OutboxEvent;
import com.dynamicmart.catalog_service.entity.OutboxStatus;
import com.dynamicmart.catalog_service.repository.OutboxEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogOutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogOutboxPublisher.class);
    private static final String OUTPUT_BINDING = "catalogEvents-out-0";

    private final OutboxEventRepository outboxRepository;
    private final StreamBridge streamBridge;

    public CatalogOutboxPublisher(OutboxEventRepository outboxRepository, StreamBridge streamBridge) {
        this.outboxRepository = outboxRepository;
        this.streamBridge = streamBridge;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:1000}")
    public void publishAvailableEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> events = new ArrayList<>(outboxRepository
                .findAllByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxStatus.PENDING, now, PageRequest.of(0, 50)));
        if (events.size() < 50) {
            events.addAll(outboxRepository.findAllByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                    OutboxStatus.FAILED, now, PageRequest.of(0, 50 - events.size())));
        }
        for (OutboxEvent event : events) publish(event, now);
    }

    private void publish(OutboxEvent event, Instant now) {
        CatalogEventMessage message = new CatalogEventMessage(event.getId(), event.getAggregateType(),
                event.getAggregateId(), event.getEventType(), event.getEventVersion(), event.getPayload(),
                event.getCorrelationId(), event.getCreatedAt());
        try {
            boolean sent = streamBridge.send(OUTPUT_BINDING, message);
            if (sent) {
                event.markPublished(now);
            } else {
                scheduleRetry(event, now, "binder returned false", null);
            }
        } catch (RuntimeException exception) {
            scheduleRetry(event, now, "publisher raised an exception", exception);
        }
    }

    private void scheduleRetry(OutboxEvent event, Instant now, String reason, RuntimeException exception) {
        long delaySeconds = Math.min(300, 1L << Math.min(event.getAttemptCount(), 8));
        event.markFailed(now.plus(delaySeconds, ChronoUnit.SECONDS));
        if (exception == null) {
            LOGGER.warn("Outbox event {} was not sent: {}", event.getId(), reason);
        } else {
            LOGGER.warn("Outbox event {} was not sent: {}", event.getId(), reason, exception);
        }
    }
}
