package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CheckoutSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
    Optional<CheckoutSession> findByIdAndCustomerId(UUID id, UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from CheckoutSession session where session.id = :id and session.customerId = :customerId")
    Optional<CheckoutSession> findOwnedForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);
}
