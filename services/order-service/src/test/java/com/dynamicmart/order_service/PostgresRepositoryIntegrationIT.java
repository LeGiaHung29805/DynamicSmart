package com.dynamicmart.order_service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dynamicmart.order_service.entity.CheckoutSession;
import com.dynamicmart.order_service.entity.CheckoutSource;
import com.dynamicmart.order_service.entity.OrderSaga;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "ORDER_DATABASE_SMOKE", matches = "true")
@EnabledIfEnvironmentVariable(
        named = "DB_URL",
        matches = "(?i)jdbc:postgresql://[^/]+/[^?]*test[^?]*(\\?.*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableTestBinder
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "app.security.jwt.hmac-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "app.clients.internal-api-key=test-internal-key",
                "app.saga-recovery.enabled=false",
                "spring.cloud.stream.bindings.paymentEvents-in-0.consumer.auto-startup=false"
        })
class PostgresRepositoryIntegrationIT {
    private static final Instant NOW = Instant.parse("2026-10-02T02:00:00Z");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CheckoutSessionRepository sessions;
    @Autowired private OrderSagaRepository sagas;
    @Autowired private OutboxEventRepository outboxEvents;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeAll
    void requireDedicatedTestDatabase() {
        String database = jdbc.queryForObject("select current_database()", String.class);
        assertTrue(database != null && database.toLowerCase(Locale.ROOT).contains("test"),
                "Repository integration test chỉ được chạy trên database riêng có chữ 'test' trong tên.");
    }

    @Test
    void checkoutSessionPessimisticLockSerializesConcurrentAdmissions() throws Exception {
        UUID customerId = UUID.randomUUID();
        CheckoutSession session = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> transaction().executeWithoutResult(status -> {
                assertTrue(sessions.findOwnedForUpdate(session.getId(), customerId).isPresent());
                firstLocked.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstLocked.await(5, TimeUnit.SECONDS));

            var second = executor.submit(() -> transaction().executeWithoutResult(status -> {
                assertTrue(sessions.findOwnedForUpdate(session.getId(), customerId).isPresent());
                secondLocked.countDown();
            }));

            assertFalse(secondLocked.await(250, TimeUnit.MILLISECONDS),
                    "Request thứ hai không được vượt qua khi Checkout row đang bị khóa.");
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(secondLocked.await(1, TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            sessions.deleteById(session.getId());
        }
    }

    @Test
    void uniqueCheckoutConstraintRejectsTwoSagasWithDifferentKeys() {
        UUID customerId = UUID.randomUUID();
        CheckoutSession session = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        OrderSaga first = saga(session.getId(), UUID.randomUUID());
        try {
            sagas.saveAndFlush(first);
            OrderSaga competing = saga(session.getId(), UUID.randomUUID());

            assertThrows(DataIntegrityViolationException.class, () -> sagas.saveAndFlush(competing));
        } finally {
            sagas.deleteAllById(List.of(first.getId()));
            sessions.deleteById(session.getId());
        }
    }

    @Test
    void uniqueIdempotencyConstraintRejectsKeyReuseAcrossSessions() {
        UUID customerId = UUID.randomUUID();
        CheckoutSession firstSession = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        CheckoutSession secondSession = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        UUID sharedKey = UUID.randomUUID();
        OrderSaga first = saga(firstSession.getId(), sharedKey);
        try {
            sagas.saveAndFlush(first);
            OrderSaga keyReuse = saga(secondSession.getId(), sharedKey);

            assertThrows(DataIntegrityViolationException.class, () -> sagas.saveAndFlush(keyReuse));
        } finally {
            sagas.deleteAllById(List.of(first.getId()));
            sessions.deleteAllById(List.of(firstSession.getId(), secondSession.getId()));
        }
    }

    @Test
    void recoveryQueryUsesSkipLockedSoWorkersClaimDifferentSagas() throws Exception {
        UUID customerId = UUID.randomUUID();
        CheckoutSession firstSession = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        CheckoutSession secondSession = sessions.saveAndFlush(session(UUID.randomUUID(), customerId));
        OrderSaga firstSaga = staleSaga(firstSession.getId());
        OrderSaga secondSaga = staleSaga(secondSession.getId());
        sagas.saveAllAndFlush(List.of(firstSaga, secondSaga));

        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<UUID> firstClaim = new AtomicReference<>();
        try {
            var firstWorker = executor.submit(() -> transaction().executeWithoutResult(status -> {
                UUID claimed = sagas.findNextRecoveryCandidateForUpdate(NOW.minusSeconds(30), NOW)
                        .orElseThrow().getId();
                firstClaim.set(claimed);
                firstLocked.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstLocked.await(5, TimeUnit.SECONDS));

            var secondWorker = executor.submit(() -> transaction().execute(status ->
                    sagas.findNextRecoveryCandidateForUpdate(NOW.minusSeconds(30), NOW)
                            .orElseThrow().getId()));
            UUID secondClaim = secondWorker.get(5, TimeUnit.SECONDS);

            assertNotNull(firstClaim.get());
            assertNotEquals(firstClaim.get(), secondClaim,
                    "SKIP LOCKED phải cho worker thứ hai claim Saga khác.");
            releaseFirst.countDown();
            firstWorker.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            sagas.deleteAllById(List.of(firstSaga.getId(), secondSaga.getId()));
            sessions.deleteAllById(List.of(firstSession.getId(), secondSession.getId()));
        }
    }

    @Test
    void outboxQueryUsesSkipLockedSoPublishersClaimDifferentEvents() throws Exception {
        OutboxEvent firstEvent = outbox(UUID.randomUUID(), NOW.minusSeconds(120));
        OutboxEvent secondEvent = outbox(UUID.randomUUID(), NOW.minusSeconds(60));
        outboxEvents.saveAllAndFlush(List.of(firstEvent, secondEvent));

        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicReference<UUID> firstClaim = new AtomicReference<>();
        try {
            var firstWorker = executor.submit(() -> transaction().executeWithoutResult(status -> {
                UUID claimed = outboxEvents.findNextPublishCandidateForUpdate(NOW)
                        .orElseThrow().getId();
                firstClaim.set(claimed);
                firstLocked.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstLocked.await(5, TimeUnit.SECONDS));

            var secondWorker = executor.submit(() -> transaction().execute(status ->
                    outboxEvents.findNextPublishCandidateForUpdate(NOW).orElseThrow().getId()));
            UUID secondClaim = secondWorker.get(5, TimeUnit.SECONDS);

            assertNotNull(firstClaim.get());
            assertNotEquals(firstClaim.get(), secondClaim,
                    "SKIP LOCKED phải cho publisher thứ hai claim Outbox event khác.");
            releaseFirst.countDown();
            firstWorker.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            outboxEvents.deleteAllById(List.of(firstEvent.getId(), secondEvent.getId()));
        }
    }

    private CheckoutSession session(UUID id, UUID customerId) {
        return CheckoutSession.create(
                id, customerId, CheckoutSource.CART, UUID.randomUUID(), "a".repeat(64),
                NOW.plusSeconds(900), NOW.minusSeconds(60));
    }

    private OrderSaga saga(UUID sessionId, UUID idempotencyKey) {
        return OrderSaga.start(
                UUID.randomUUID(), sessionId, idempotencyKey, "b".repeat(64), UUID.randomUUID(), NOW);
    }

    private OrderSaga staleSaga(UUID sessionId) {
        OrderSaga saga = saga(sessionId, UUID.randomUUID());
        saga.setUpdatedAt(NOW.minusSeconds(120));
        return saga;
    }

    private OutboxEvent outbox(UUID id, Instant createdAt) {
        return OutboxEvent.pending(
                id, "ORDER", UUID.randomUUID(), "OrderCompleted", 1, "{}", UUID.randomUUID(), createdAt);
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactions);
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Hết thời gian chờ coordination latch.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread integration test bị interrupt.", exception);
        }
    }
}
