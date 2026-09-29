package com.dynamicmart.order_service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@EnabledIfEnvironmentVariable(named = "ORDER_DATABASE_SMOKE", matches = "true")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "app.security.jwt.hmac-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "app.clients.internal-api-key=test-internal-key"
        })
class DatabaseMigrationSmokeIT {
    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayAndHibernateValidateSagaIdempotencySchema() {
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

        assertEquals(2, columns);
        assertEquals(1, uniqueIndex);
    }
}
