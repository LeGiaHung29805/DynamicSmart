package com.dynamicmart.cart_service.repository;

import com.dynamicmart.cart_service.entity.PromotionAudit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionAuditRepository extends JpaRepository<PromotionAudit, UUID> {
    Optional<PromotionAudit> findByActorAdminIdAndIdempotencyKey(UUID actorAdminId, UUID idempotencyKey);
    List<PromotionAudit> findAllByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, UUID targetId);
}
