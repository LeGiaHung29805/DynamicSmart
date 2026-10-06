package com.dynamicmart.order_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.jdbc.core.JdbcTemplate;

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
class DatabaseMigrationSmokeIT {
    @Autowired private JdbcTemplate jdbc;

    @BeforeAll
    void requireDedicatedTestDatabase() {
        String database = jdbc.queryForObject("select current_database()", String.class);
        assertTrue(database != null && database.toLowerCase(java.util.Locale.ROOT).contains("test"),
                "Database smoke chỉ được chạy trên database riêng có chữ 'test' trong tên.");
    }

    @Test
    void flywayAndHibernateValidateSagaIdempotencyAndPaymentSchema() {
        Integer columns = jdbc.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'order_sagas'
                  and column_name in ('idempotency_key', 'request_hash')
                  and is_nullable = 'NO'
                """, Integer.class);
        Integer uniqueIndex = jdbc.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = 'public'
                  and tablename = 'order_sagas'
                  and indexname = 'uq_order_sagas_idempotency_key'
                """, Integer.class);
        Integer paymentColumn = jdbc.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'order_sagas'
                  and column_name = 'payment_id'
                """, Integer.class);
        Integer paymentIndex = jdbc.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = 'public'
                  and tablename = 'order_sagas'
                  and indexname = 'uq_order_sagas_payment_id'
                """, Integer.class);
        Integer recoveryColumns = jdbc.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'order_sagas'
                  and column_name in ('recovery_owner', 'recovery_lease_until')
                """, Integer.class);
        Integer recoveryIndex = jdbc.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = 'public'
                  and tablename = 'order_sagas'
                  and indexname = 'idx_order_sagas_recovery_due'
                """, Integer.class);
        Integer outboxLeaseColumns = jdbc.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'outbox_events'
                  and column_name in ('processing_owner', 'processing_lease_until')
                """, Integer.class);
        Integer outboxClaimIndex = jdbc.queryForObject("""
                select count(*)
                from pg_indexes
                where schemaname = 'public'
                  and tablename = 'outbox_events'
                  and indexname = 'idx_outbox_events_publish_claim'
                """, Integer.class);
        String sagaStatusConstraint = jdbc.queryForObject("""
                select pg_get_constraintdef(oid)
                from pg_constraint
                where conrelid = 'order_sagas'::regclass
                  and conname = 'order_sagas_status_check'
                """, String.class);

        assertEquals(2, columns);
        assertEquals(1, uniqueIndex);
        assertEquals(1, paymentColumn);
        assertEquals(1, paymentIndex);
        assertEquals(2, recoveryColumns);
        assertEquals(1, recoveryIndex);
        assertEquals(2, outboxLeaseColumns);
        assertEquals(1, outboxClaimIndex);
        assertTrue(sagaStatusConstraint.contains("FINALIZING_RESERVATIONS"));
        assertTrue(sagaStatusConstraint.contains("INVENTORY_COMMITTED"));
    }
}
