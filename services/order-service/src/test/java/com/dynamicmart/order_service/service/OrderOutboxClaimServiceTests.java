package com.dynamicmart.order_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderOutboxClaimServiceTests {
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID CORRELATION_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final UUID OLD_OWNER = UUID.fromString("50000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-02T04:00:00Z");

    @Test
    void claimMovesPendingEventToProcessingWithLease() {
        Fixture fixture = fixture();
        when(fixture.events().findNextPublishCandidateForUpdate(NOW))
                .thenReturn(Optional.of(fixture.event()));

        var claim = fixture.service().claimNext().orElseThrow();

        assertEquals(EVENT_ID, claim.eventId());
        assertEquals(OutboxEventStatus.PROCESSING, fixture.event().getStatus());
        assertEquals(claim.owner(), fixture.event().getProcessingOwner());
        assertEquals(NOW.plusSeconds(30), fixture.event().getProcessingLeaseUntil());
        verify(fixture.events()).save(fixture.event());
    }

    @Test
    void activeLeaseCannotBeClaimedAgain() {
        Fixture fixture = fixture();
        processing(fixture.event(), OLD_OWNER, NOW.plusSeconds(1));
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        assertTrue(fixture.service().claim(EVENT_ID).isEmpty());
    }

    @Test
    void expiredLeaseCanBeReclaimedByAnotherWorker() {
        Fixture fixture = fixture();
        processing(fixture.event(), OLD_OWNER, NOW.minusSeconds(1));
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        var claim = fixture.service().claim(EVENT_ID).orElseThrow();

        assertNotEquals(OLD_OWNER, claim.owner());
        assertEquals(claim.owner(), fixture.event().getProcessingOwner());
        assertEquals(NOW.plusSeconds(30), fixture.event().getProcessingLeaseUntil());
    }

    @Test
    void staleWorkerCannotMarkNewOwnersClaimAsPublished() {
        Fixture fixture = fixture();
        UUID newOwner = UUID.randomUUID();
        processing(fixture.event(), newOwner, NOW.plusSeconds(30));
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        assertFalse(fixture.service().markPublished(EVENT_ID, OLD_OWNER));
        assertEquals(OutboxEventStatus.PROCESSING, fixture.event().getStatus());
        assertEquals(newOwner, fixture.event().getProcessingOwner());
    }

    @Test
    void ownerMarksPublishedAndClearsLease() {
        Fixture fixture = fixture();
        processing(fixture.event(), OLD_OWNER, NOW.plusSeconds(30));
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        assertTrue(fixture.service().markPublished(EVENT_ID, OLD_OWNER));

        assertEquals(OutboxEventStatus.PUBLISHED, fixture.event().getStatus());
        assertEquals(NOW, fixture.event().getPublishedAt());
        assertNull(fixture.event().getProcessingOwner());
        assertNull(fixture.event().getProcessingLeaseUntil());
    }

    @Test
    void failureReturnsEventToPendingWithBackoff() {
        Fixture fixture = fixture();
        processing(fixture.event(), OLD_OWNER, NOW.plusSeconds(30));
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        assertTrue(fixture.service().markFailed(EVENT_ID, OLD_OWNER));

        assertEquals(OutboxEventStatus.PENDING, fixture.event().getStatus());
        assertEquals(1, fixture.event().getAttemptCount());
        assertTrue(fixture.event().getAvailableAt().isAfter(NOW));
        assertNull(fixture.event().getProcessingOwner());
    }

    @Test
    void tenthFailureMovesEventToTerminalFailedState() {
        Fixture fixture = fixture();
        processing(fixture.event(), OLD_OWNER, NOW.plusSeconds(30));
        fixture.event().setAttemptCount(9);
        when(fixture.events().findByIdForUpdate(EVENT_ID)).thenReturn(Optional.of(fixture.event()));

        assertTrue(fixture.service().markFailed(EVENT_ID, OLD_OWNER));

        assertEquals(OutboxEventStatus.FAILED, fixture.event().getStatus());
        assertEquals(10, fixture.event().getAttemptCount());
        assertNotNull(fixture.event().getAvailableAt());
    }

    private Fixture fixture() {
        OutboxEventRepository events = Mockito.mock(OutboxEventRepository.class);
        OutboxEvent event = OutboxEvent.pending(
                EVENT_ID, "ORDER", ORDER_ID, "OrderCompleted", 1, "{}",
                CORRELATION_ID, NOW.minusSeconds(10));
        OrderOutboxClaimService service = new OrderOutboxClaimService(
                events, new OutboxProperties(Duration.ofSeconds(1), 100, Duration.ofSeconds(30)),
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(service, events, event);
    }

    private void processing(OutboxEvent event, UUID owner, Instant leaseUntil) {
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setProcessingOwner(owner);
        event.setProcessingLeaseUntil(leaseUntil);
    }

    private record Fixture(
            OrderOutboxClaimService service,
            OutboxEventRepository events,
            OutboxEvent event) {
    }
}
