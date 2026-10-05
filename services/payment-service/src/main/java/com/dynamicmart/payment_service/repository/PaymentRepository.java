package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.Payment;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    List<Payment> findByStatusAndTimingAndExpiresAtBefore(String status, String timing, Instant instant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.status = :status and p.timing = :timing and p.expiresAt < :instant")
    List<Payment> findDueForUpdate(@Param("status") String status, @Param("timing") String timing, @Param("instant") Instant instant);

    boolean existsByProviderTransactionRefAndIdNot(String providerTransactionRef, UUID id);
}
