package com.dynamicmart.identity.repository;

import com.dynamicmart.identity.entity.UserManagementAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserManagementAuditRepository extends JpaRepository<UserManagementAudit, UUID> {
    boolean existsByActorAdminIdAndIdempotencyKey(UUID actorAdminId, UUID idempotencyKey);
    List<UserManagementAudit> findAllByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
