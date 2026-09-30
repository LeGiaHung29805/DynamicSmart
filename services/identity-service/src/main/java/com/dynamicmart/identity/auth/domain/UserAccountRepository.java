package com.dynamicmart.identity.auth.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByEmailNormalized(String emailNormalized);
    boolean existsByEmailNormalized(String emailNormalized);

    @Query("select count(user) from UserAccount user where user.role = com.dynamicmart.identity.auth.domain.UserRole.ADMIN and user.status = com.dynamicmart.identity.auth.domain.UserStatus.ACTIVE")
    long countActiveAdmins();
}
