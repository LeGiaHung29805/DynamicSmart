package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.Payment;
import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    List<Payment> findByStatusAndTimingAndExpiresAtBefore(String status, String timing, Instant instant);
}
