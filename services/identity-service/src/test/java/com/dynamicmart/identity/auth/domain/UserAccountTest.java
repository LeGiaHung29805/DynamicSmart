package com.dynamicmart.identity.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserAccountTest {
    @Test
    void profileChangeDoesNotInvalidateSessions() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UserAccount user = new UserAccount(UUID.randomUUID(), "thao@example.com", "thao@example.com", "hash", "Thảo", now);

        user.updateProfile("Nguyễn Thảo", "0900000000", now.plus(1, ChronoUnit.MINUTES));

        assertThat(user.getFullName()).isEqualTo("Nguyễn Thảo");
        assertThat(user.getPhone()).isEqualTo("0900000000");
        assertThat(user.getAuthVersion()).isZero();
    }

    @Test
    void roleAndStatusChangesInvalidateExistingSessions() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UserAccount user = new UserAccount(UUID.randomUUID(), "thao@example.com", "thao@example.com", "hash", "Thảo", now);

        user.changeRole(UserRole.ADMIN, now.plusSeconds(1));
        user.changeStatus(UserStatus.LOCKED, now.plusSeconds(2));

        assertThat(user.getAuthVersion()).isEqualTo(2);
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
    }
}
