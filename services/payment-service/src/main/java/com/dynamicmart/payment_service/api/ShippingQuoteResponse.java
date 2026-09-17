package com.dynamicmart.payment_service.api;

import java.time.Instant;
import java.util.UUID;

public record ShippingQuoteResponse(UUID quoteId, long feeVnd, long shippingDiscountVnd, long payableFeeVnd,
                                    int serviceId, String serviceName, String eta, Instant expiresAt) { }
