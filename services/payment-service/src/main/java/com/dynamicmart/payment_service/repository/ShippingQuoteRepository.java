package com.dynamicmart.payment_service.repository;

import com.dynamicmart.payment_service.entity.ShippingQuote;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, UUID> {
    Optional<ShippingQuote> findByIdAndCustomerId(UUID id, UUID customerId);
    Optional<ShippingQuote> findByRequestFingerprintAndCustomerIdAndExpiresAtAfterAndUsedAtIsNull(String requestFingerprint, UUID customerId, Instant now);
}
