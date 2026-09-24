package com.dynamicmart.catalog_service.service;

import java.sql.PreparedStatement;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresAdvisoryLock {
    private final JdbcTemplate jdbcTemplate;

    public PostgresAdvisoryLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lock(UUID operationKey) {
        long lockKey = operationKey.getMostSignificantBits() ^ operationKey.getLeastSignificantBits();
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, lockKey);
                statement.execute();
            }
            return null;
        });
    }
}
