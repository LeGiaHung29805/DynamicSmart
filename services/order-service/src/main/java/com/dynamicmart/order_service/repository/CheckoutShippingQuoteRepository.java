package com.dynamicmart.order_service.repository;

import com.dynamicmart.order_service.entity.CheckoutShippingQuote;
import com.dynamicmart.order_service.entity.ShippingQuoteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutShippingQuoteRepository extends JpaRepository<CheckoutShippingQuote, UUID> {
    Optional<CheckoutShippingQuote> findFirstByCheckoutSessionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID checkoutSessionId, ShippingQuoteStatus status, Instant now);

    Optional<CheckoutShippingQuote> findByQuoteIdAndStatus(UUID quoteId, ShippingQuoteStatus status);
    Optional<CheckoutShippingQuote> findByProviderAndQuoteId(String provider, UUID quoteId);
    List<CheckoutShippingQuote> findAllByCheckoutSessionIdAndStatus(UUID checkoutSessionId, ShippingQuoteStatus status);
}
