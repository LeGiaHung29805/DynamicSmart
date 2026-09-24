package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutShippingQuoteRepository extends JpaRepository<CheckoutShippingQuote, UUID> {
    Optional<CheckoutShippingQuote> findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID checkoutSessionId, ShippingQuoteStatus status, Instant now);

    Optional<CheckoutShippingQuote> findByQuoteIdAndStatus(UUID quoteId, ShippingQuoteStatus status);
    Optional<CheckoutShippingQuote> findByProviderAndQuoteId(String provider, UUID quoteId);
    List<CheckoutShippingQuote> findAllByCheckoutSessionIdAndStatus(UUID checkoutSessionId, ShippingQuoteStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quote from CheckoutShippingQuote quote where quote.id = :id")
    Optional<CheckoutShippingQuote> findByIdForUpdate(@Param("id") UUID id);
}
