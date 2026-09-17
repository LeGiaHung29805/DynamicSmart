package com.dynamicmart.payment_service.api;

import com.dynamicmart.payment_service.application.ShippingQuoteService;
import com.dynamicmart.payment_service.application.InternalApiVerifier;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
public class ShippingController {
    private final ShippingQuoteService shipping;
    private final InternalApiVerifier internalApi;
    public ShippingController(ShippingQuoteService shipping, InternalApiVerifier internalApi) { this.shipping = shipping; this.internalApi = internalApi; }

    /** Internal Order Service endpoint; public clients obtain its result through Checkout Preview. */
    @PostMapping("/quotes")
    public ShippingQuoteResponse quote(@RequestHeader("X-Internal-Api-Key") String apiKey, @Valid @RequestBody ShippingQuoteRequest request) { internalApi.require(apiKey); return shipping.quote(request); }

    @PostMapping("/quotes/{quoteId}/validate")
    public ShippingQuoteResponse validateAndUse(@RequestHeader("X-Internal-Api-Key") String apiKey, @PathVariable UUID quoteId, @Valid @RequestBody QuoteValidationRequest request) { internalApi.require(apiKey); return shipping.validateAndUse(quoteId, request); }
}
