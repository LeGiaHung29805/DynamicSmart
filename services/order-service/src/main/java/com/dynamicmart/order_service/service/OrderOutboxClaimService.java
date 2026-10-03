package com.dynamicmart.order_service.service;

import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderOutboxClaimService {
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository events;
    private final OutboxProperties properties;
    private final Clock clock;

    public OrderOutboxClaimService(
            OutboxEventRepository events,
            OutboxProperties properties,
            Clock clock) {
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Optional<OutboxClaim> claimNext() {
        Instant now = Instant.now(clock);
        return events.findNextPublishCandidateForUpdate(now).map(event -> claim(event, now));
    }

    @Transactional
    public Optional<OutboxClaim> claim(UUID eventId) {
        Instant now = Instant.now(clock);
        return events.findByIdForUpdate(eventId)
                .filter(event -> eligible(event, now))
                .map(event -> claim(event, now));
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID owner) {
        OutboxEvent event = ownedClaim(eventId, owner).orElse(null);
        if (event == null) {
            return false;
        }
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now(clock));
        clearLease(event);
        events.save(event);
        return true;
    }

    @Transactional
    public boolean markFailed(UUID eventId, UUID owner) {
        OutboxEvent event = ownedClaim(eventId, owner).orElse(null);
        if (event == null) {
            return false;
        }
        Instant now = Instant.now(clock);
        int attempt = Math.addExact(event.getAttemptCount(), 1);
        event.setAttemptCount(attempt);
        event.setStatus(attempt >= MAX_ATTEMPTS ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING);
        event.setAvailableAt(now.plusSeconds(Math.min(300, 1L << Math.min(attempt, 8))));
        clearLease(event);
        events.save(event);
        return true;
    }

    private OutboxClaim claim(OutboxEvent event, Instant now) {
        UUID owner = UUID.randomUUID();
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setProcessingOwner(owner);
        event.setProcessingLeaseUntil(now.plus(properties.processingLease()));
        events.save(event);
        return new OutboxClaim(
                event.getId(), owner, event.getAggregateId(), event.getEventType(), event.getEventVersion(),
                event.getPayload(), event.getCorrelationId(), event.getCreatedAt());
    }

    private boolean eligible(OutboxEvent event, Instant now) {
        if (event.getStatus() == OutboxEventStatus.PENDING) {
            return !event.getAvailableAt().isAfter(now);
        }
        return event.getStatus() == OutboxEventStatus.PROCESSING
                && (event.getProcessingLeaseUntil() == null || !event.getProcessingLeaseUntil().isAfter(now));
    }

    private Optional<OutboxEvent> ownedClaim(UUID eventId, UUID owner) {
        return events.findByIdForUpdate(eventId)
                .filter(event -> event.getStatus() == OutboxEventStatus.PROCESSING)
                .filter(event -> owner.equals(event.getProcessingOwner()));
    }

    private void clearLease(OutboxEvent event) {
        event.setProcessingOwner(null);
        event.setProcessingLeaseUntil(null);
    }

    public record OutboxClaim(
            UUID eventId,
            UUID owner,
            UUID aggregateId,
            String eventType,
            int eventVersion,
            String payload,
            UUID correlationId,
            Instant createdAt) {
    }
}
