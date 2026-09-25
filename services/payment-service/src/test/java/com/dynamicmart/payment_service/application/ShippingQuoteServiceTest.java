package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.api.QuoteValidationRequest;
import com.dynamicmart.payment_service.config.GhnProperties;
import com.dynamicmart.payment_service.entity.ShippingQuote;
import com.dynamicmart.payment_service.repository.ShippingQuoteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingQuoteServiceTest {
    private ShippingQuoteRepository quotes;
    private ShippingQuoteService service;

    @BeforeEach
    void setUp() {
        quotes = mock(ShippingQuoteRepository.class);
        service = new ShippingQuoteService(mock(LocationService.class), mock(GhnClient.class), quotes,
                new GhnProperties("https://example.test", "token", "shop", 1, "ward", 15), new ObjectMapper());
    }

    @Test
    void consumesMatchingQuoteUsingWriteLock() {
        UUID customerId = UUID.randomUUID(); ShippingQuote quote = quote(customerId);
        when(quotes.findByIdAndCustomerIdForUpdate(quote.getId(), customerId)).thenReturn(Optional.of(quote));

        service.validateAndUse(quote.getId(), new QuoteValidationRequest(customerId, quote.getRequestFingerprint()));

        assertNotNull(quote.getUsedAt());
        verify(quotes).save(quote);
    }

    @Test
    void rejectsConsumedQuote() {
        UUID customerId = UUID.randomUUID(); ShippingQuote quote = quote(customerId); quote.setUsedAt(Instant.now());
        when(quotes.findByIdAndCustomerIdForUpdate(quote.getId(), customerId)).thenReturn(Optional.of(quote));

        PaymentException error = assertThrows(PaymentException.class, () -> service.validateAndUse(quote.getId(),
                new QuoteValidationRequest(customerId, quote.getRequestFingerprint())));

        assertEquals("SHIPPING_QUOTE_INVALID", error.getCode());
    }

    private ShippingQuote quote(UUID customerId) {
        ShippingQuote quote = new ShippingQuote(); quote.setId(UUID.randomUUID()); quote.setCustomerId(customerId); quote.setRequestFingerprint("a".repeat(64));
        quote.setFeeVnd(30_000); quote.setShippingDiscountVnd(0); quote.setServiceId(2); quote.setServiceName("GHN"); quote.setExpiresAt(Instant.now().plusSeconds(600)); quote.setCreatedAt(Instant.now()); return quote;
    }
}
