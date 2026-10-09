package com.dynamicmart.payment_service.service;

import com.dynamicmart.payment_service.client.GhnClient;
import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.dto.request.QuoteValidationRequest;
import com.dynamicmart.payment_service.dto.request.ShippingItemRequest;
import com.dynamicmart.payment_service.dto.request.ShippingQuoteRequest;
import com.dynamicmart.payment_service.config.GhnProperties;
import com.dynamicmart.payment_service.entity.ShippingQuote;
import com.dynamicmart.payment_service.repository.ShippingQuoteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ShippingQuoteServiceTest {
    private ShippingQuoteRepository quotes;
    private LocationService locations;
    private GhnClient ghn;
    private ShippingQuoteService service;

    @BeforeEach
    void setUp() {
        quotes = mock(ShippingQuoteRepository.class);
        locations = mock(LocationService.class);
        ghn = mock(GhnClient.class);
        service = new ShippingQuoteService(locations, ghn, quotes,
                new GhnProperties("https://example.test", "token", "shop", 1, "ward", 15), new ObjectMapper());
    }

    @Test
    void scopesReusableQuoteToCheckoutSession() {
        UUID customerId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        var first = quoteRequest(customerId, UUID.randomUUID(), variantId);
        var second = quoteRequest(customerId, UUID.randomUUID(), variantId);
        when(locations.validateForQuote(201, 11007))
                .thenReturn(new LocationService.ValidatedLocation(201, 1482, 11007, "1A0101"));
        when(ghn.quote(org.mockito.ArgumentMatchers.eq(1482), org.mockito.ArgumentMatchers.eq("1A0101"),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new GhnClient.Rate(30_000, 2, "GHN", "2-3 ngày"));
        when(quotes.findByRequestFingerprintAndCustomerIdAndExpiresAtAfterAndUsedAtIsNull(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(customerId),
                org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(Optional.empty());

        service.quote(first);
        service.quote(second);

        ArgumentCaptor<String> fingerprints = ArgumentCaptor.forClass(String.class);
        verify(quotes, times(2)).findByRequestFingerprintAndCustomerIdAndExpiresAtAfterAndUsedAtIsNull(
                fingerprints.capture(), org.mockito.ArgumentMatchers.eq(customerId),
                org.mockito.ArgumentMatchers.any(Instant.class));
        assertEquals(2, fingerprints.getAllValues().stream().distinct().count());
        verify(quotes, times(2)).save(org.mockito.ArgumentMatchers.any(ShippingQuote.class));
    }

    @Test
    void consumesMatchingQuoteUsingWriteLock() {
        UUID customerId = UUID.randomUUID(); ShippingQuote quote = quote(customerId);
        when(quotes.findByIdAndCustomerIdForUpdate(quote.getId(), customerId)).thenReturn(Optional.of(quote));

        var response = service.validateAndUse(quote.getId(), new QuoteValidationRequest(customerId, quote.getRequestFingerprint()));

        assertNotNull(quote.getUsedAt());
        assertEquals(quote.getRequestFingerprint(), response.requestFingerprint());
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

    private ShippingQuoteRequest quoteRequest(UUID customerId, UUID checkoutSessionId, UUID variantId) {
        return new ShippingQuoteRequest(customerId, checkoutSessionId, 201, 11007,
                List.of(new ShippingItemRequest(variantId, 1, 500, 20, 10, 5)), 0L, null);
    }
}
