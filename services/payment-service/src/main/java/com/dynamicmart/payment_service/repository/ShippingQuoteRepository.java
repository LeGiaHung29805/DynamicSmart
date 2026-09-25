package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.ShippingQuote;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, UUID> {
    Optional<ShippingQuote> findByIdAndCustomerId(UUID id, UUID customerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from ShippingQuote q where q.id = :id and q.customerId = :customerId")
    Optional<ShippingQuote> findByIdAndCustomerIdForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);
    Optional<ShippingQuote> findByRequestFingerprintAndCustomerIdAndExpiresAtAfterAndUsedAtIsNull(String requestFingerprint, UUID customerId, Instant now);
}
