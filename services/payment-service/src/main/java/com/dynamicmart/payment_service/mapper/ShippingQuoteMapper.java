package com.dynamicmart.payment_service.mapper;

import com.dynamicmart.payment_service.dto.response.ShippingQuoteResponse;
import com.dynamicmart.payment_service.entity.ShippingQuote;

public final class ShippingQuoteMapper {
    private ShippingQuoteMapper() { }

    public static ShippingQuoteResponse toResponse(ShippingQuote quote) {
        return new ShippingQuoteResponse(
                quote.getId(), quote.getFeeVnd(), quote.getShippingDiscountVnd(),
                quote.getFeeVnd() - quote.getShippingDiscountVnd(), quote.getServiceId(),
                quote.getServiceName(), quote.getEtaText(), quote.getExpiresAt(),
                quote.getRequestFingerprint());
    }
}
