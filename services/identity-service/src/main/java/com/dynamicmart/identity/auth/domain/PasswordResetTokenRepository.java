package com.dynamicmart.identity.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken token set token.usedAt = :now " +
            "where token.user.id = :userId and token.usedAt is null")
    void invalidateUnusedByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
