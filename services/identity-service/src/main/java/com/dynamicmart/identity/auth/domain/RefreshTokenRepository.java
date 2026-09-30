package com.dynamicmart.identity.auth.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, java.util.UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :now where token.familyId = :familyId and token.revokedAt is null")
    void revokeFamily(@Param("familyId") java.util.UUID familyId, @Param("now") java.time.Instant now);

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :now where token.user.id = :userId and token.revokedAt is null")
    void revokeAllByUserId(@Param("userId") java.util.UUID userId, @Param("now") java.time.Instant now);
}
