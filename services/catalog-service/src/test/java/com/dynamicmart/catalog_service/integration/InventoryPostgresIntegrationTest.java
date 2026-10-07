package com.dynamicmart.catalog_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dynamicmart.catalog_service.dto.request.AdjustInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.CommitInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.InventoryReservationItemRequest;
import com.dynamicmart.catalog_service.dto.request.ReleaseInventoryRequest;
import com.dynamicmart.catalog_service.dto.request.ReserveInventoryRequest;
import com.dynamicmart.catalog_service.dto.response.InventoryReservationResponse;
import com.dynamicmart.catalog_service.entity.InventoryReservationStatus;
import com.dynamicmart.catalog_service.exception.CatalogException;
import com.dynamicmart.catalog_service.service.InventoryAdjustmentService;
import com.dynamicmart.catalog_service.service.InventoryExpirationScheduler;
import com.dynamicmart.catalog_service.service.InventoryReservationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.cloud.stream.output-bindings=",
        "app.outbox.enabled=false",
        "app.promotion.mode=DISABLED",
        "app.inventory.expiration-scan-ms=3600000",
        "app.security.jwt.issuer=dynamicmart-identity-service",
        "app.security.jwt.hmac-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.internal.api-key=test-internal-key"
})
class InventoryPostgresIntegrationTest {
    private static final UUID CATEGORY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID VARIANT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("catalog_test")
            .withUsername("catalog_test")
            .withPassword("catalog_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private InventoryReservationService reservationService;
    @Autowired private InventoryAdjustmentService adjustmentService;
    @Autowired private InventoryExpirationScheduler expirationScheduler;
    @Autowired private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        jdbcTemplate.execute("""
                TRUNCATE TABLE inventory_operation_log, inventory_adjustments,
                  inventory_reservation_items, inventory_reservations, inventory_items,
                  variant_attribute_values, product_attribute_values, product_images,
                  product_variants, products, category_attributes, attribute_options,
                  attributes, categories, outbox_events, processed_events, idempotency_records
                CASCADE
                """);
        insertPurchasableVariant(1);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void onlyOneConcurrentBuyerCanReserveTheLastItem() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Attempt> first = executor.submit(() -> reserveWhenReleased(ready, start));
        Future<Attempt> second = executor.submit(() -> reserveWhenReleased(ready, start));
        ready.await();
        start.countDown();

        List<Attempt> attempts = List.of(first.get(), second.get());

        assertThat(attempts).filteredOn(Attempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> "INSUFFICIENT_INVENTORY".equals(attempt.errorCode()))
                .hasSize(1);
        assertThat(integer("SELECT reserved_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(1);
        assertThat(integer("SELECT on_hand_qty - reserved_qty FROM inventory_items WHERE variant_id = ?",
                VARIANT_ID)).isZero();
        assertThat(integer("SELECT COUNT(*) FROM inventory_reservations")).isEqualTo(1);
    }

    @Test
    void reserveCommitAndReleaseAreIdempotentAndRejectChangedPayload() {
        jdbcTemplate.update("UPDATE inventory_items SET on_hand_qty = 2 WHERE variant_id = ?", VARIANT_ID);
        Instant expiresAt = Instant.now().plusSeconds(900);
        UUID reserveKey = UUID.randomUUID();
        ReserveInventoryRequest reserveRequest = request(UUID.randomUUID(), expiresAt, 1);

        InventoryReservationResponse reserved = reservationService.reserve(reserveKey, reserveRequest);
        InventoryReservationResponse reserveReplay = reservationService.reserve(reserveKey, reserveRequest);

        assertThat(reserveReplay).isEqualTo(reserved);
        assertThat(integer("SELECT reserved_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(1);
        assertThatThrownBy(() -> reservationService.reserve(reserveKey,
                request(UUID.randomUUID(), expiresAt, 1)))
                .isInstanceOfSatisfying(CatalogException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("IDEMPOTENCY_PAYLOAD_MISMATCH"));

        UUID orderId = UUID.randomUUID();
        UUID commitKey = UUID.randomUUID();
        CommitInventoryRequest commitRequest = new CommitInventoryRequest(orderId);
        InventoryReservationResponse committed = reservationService.commit(
                reserved.reservationId(), commitKey, commitRequest);
        InventoryReservationResponse commitReplay = reservationService.commit(
                reserved.reservationId(), commitKey, commitRequest);

        assertThat(commitReplay).isEqualTo(committed);
        assertThat(committed.status()).isEqualTo(InventoryReservationStatus.COMMITTED);
        assertThat(integer("SELECT on_hand_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(1);
        assertThat(integer("SELECT reserved_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isZero();

        UUID secondReserveKey = UUID.randomUUID();
        InventoryReservationResponse secondReservation = reservationService.reserve(secondReserveKey,
                request(UUID.randomUUID(), Instant.now().plusSeconds(900), 1));
        UUID releaseKey = UUID.randomUUID();
        ReleaseInventoryRequest releaseRequest = new ReleaseInventoryRequest("PAYMENT_FAILED");
        InventoryReservationResponse released = reservationService.release(
                secondReservation.reservationId(), releaseKey, releaseRequest);
        InventoryReservationResponse releaseReplay = reservationService.release(
                secondReservation.reservationId(), releaseKey, releaseRequest);

        assertThat(releaseReplay).isEqualTo(released);
        assertThat(released.status()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(integer("SELECT on_hand_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(1);
        assertThat(integer("SELECT reserved_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isZero();
    }

    @Test
    void expirationReleasesStockExactlyOnce() {
        InventoryReservationResponse reservation = reservationService.reserve(UUID.randomUUID(),
                request(UUID.randomUUID(), Instant.now().plusSeconds(900), 1));
        jdbcTemplate.update("UPDATE inventory_reservations SET expires_at = NOW() - INTERVAL '1 second' "
                + "WHERE id = ?", reservation.reservationId());

        expirationScheduler.expireDueReservations();
        expirationScheduler.expireDueReservations();

        assertThat(reservationService.get(reservation.reservationId()).status())
                .isEqualTo(InventoryReservationStatus.EXPIRED);
        assertThat(integer("SELECT reserved_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isZero();
        assertThat(integer("SELECT COUNT(*) FROM inventory_operation_log WHERE operation_type = 'RELEASE'"))
                .isEqualTo(1);
    }

    @Test
    void adjustmentCannotReduceOnHandBelowReservedAndReplayDoesNotApplyTwice() {
        reservationService.reserve(UUID.randomUUID(),
                request(UUID.randomUUID(), Instant.now().plusSeconds(900), 1));
        UUID actorId = UUID.randomUUID();

        assertThatThrownBy(() -> adjustmentService.adjust(VARIANT_ID, UUID.randomUUID(), actorId,
                new AdjustInventoryRequest(-1, "Không được thấp hơn lượng đã giữ")))
                .isInstanceOfSatisfying(CatalogException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("INVENTORY_ADJUSTMENT_REJECTED"));
        assertThat(integer("SELECT on_hand_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(1);

        UUID operationKey = UUID.randomUUID();
        AdjustInventoryRequest increase = new AdjustInventoryRequest(2, "Nhập bổ sung");
        var first = adjustmentService.adjust(VARIANT_ID, operationKey, actorId, increase);
        var replay = adjustmentService.adjust(VARIANT_ID, operationKey, actorId, increase);

        assertThat(replay).isEqualTo(first);
        assertThat(integer("SELECT on_hand_qty FROM inventory_items WHERE variant_id = ?", VARIANT_ID))
                .isEqualTo(3);
        assertThat(integer("SELECT COUNT(*) FROM inventory_adjustments WHERE operation_key = ?", operationKey))
                .isEqualTo(1);
    }

    private Attempt reserveWhenReleased(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            InventoryReservationResponse response = reservationService.reserve(UUID.randomUUID(),
                    request(UUID.randomUUID(), Instant.now().plusSeconds(900), 1));
            return new Attempt(true, response.reservationId(), null);
        } catch (CatalogException exception) {
            return new Attempt(false, null, exception.getCode());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private ReserveInventoryRequest request(UUID checkoutId, Instant expiresAt, int quantity) {
        return new ReserveInventoryRequest(checkoutId, expiresAt,
                List.of(new InventoryReservationItemRequest(VARIANT_ID, quantity)));
    }

    private void insertPurchasableVariant(int onHandQuantity) {
        jdbcTemplate.update("""
                INSERT INTO categories (id, code, name, slug, status, sort_order)
                VALUES (?, 'TEST', 'Kiểm thử', 'kiem-thu', 'ACTIVE', 0)
                """, CATEGORY_ID);
        jdbcTemplate.update("""
                INSERT INTO products (id, category_id, name, slug, status, published_at,
                  is_featured, default_weight_grams, default_length_cm, default_width_cm,
                  default_height_cm)
                VALUES (?, ?, 'Sản phẩm cuối', 'san-pham-cuoi', 'ACTIVE', NOW() - INTERVAL '1 hour',
                  FALSE, 500, 10, 10, 10)
                """, PRODUCT_ID, CATEGORY_ID);
        jdbcTemplate.update("""
                INSERT INTO product_variants (id, product_id, sku, name, price_vnd,
                  weight_grams, length_cm, width_cm, height_cm, status, sort_order)
                VALUES (?, ?, 'LAST-ONE', 'Sản phẩm cuối', 100000, 500, 10, 10, 10, 'ACTIVE', 0)
                """, VARIANT_ID, PRODUCT_ID);
        jdbcTemplate.update("INSERT INTO inventory_items (variant_id, on_hand_qty, reserved_qty) "
                + "VALUES (?, ?, 0)", VARIANT_ID, onHandQuantity);
    }

    private int integer(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }

    private record Attempt(boolean success, UUID reservationId, String errorCode) {
    }
}
