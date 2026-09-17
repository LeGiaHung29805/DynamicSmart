package com.dynamicmart.payment_service.application;

import com.dynamicmart.payment_service.api.PaymentException;
import com.dynamicmart.payment_service.api.QuoteValidationRequest;
import com.dynamicmart.payment_service.api.ShippingQuoteRequest;
import com.dynamicmart.payment_service.api.ShippingQuoteResponse;
import com.dynamicmart.payment_service.config.GhnProperties;
import com.dynamicmart.payment_service.entity.ShippingQuote;
import com.dynamicmart.payment_service.repository.ShippingQuoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingQuoteService {
    private final LocationService locations;
    private final GhnClient ghn;
    private final ShippingQuoteRepository quotes;
    private final GhnProperties properties;
    private final ObjectMapper objectMapper;

    public ShippingQuoteService(LocationService locations, GhnClient ghn, ShippingQuoteRepository quotes, GhnProperties properties, ObjectMapper objectMapper) {
        this.locations = locations; this.ghn = ghn; this.quotes = quotes; this.properties = properties; this.objectMapper = objectMapper;
    }

    @Transactional
    public ShippingQuoteResponse quote(ShippingQuoteRequest request) {
        locations.validate(request.provinceId(), request.wardId());
        String fingerprint = fingerprint(request); Instant now = Instant.now();
        var existing = quotes.findByRequestFingerprintAndCustomerIdAndExpiresAtAfterAndUsedAtIsNull(fingerprint, request.customerId(), now);
        if (existing.isPresent()) return response(existing.get());
        GhnClient.Rate rate = ghn.quote(request.wardId(), request.items());
        if (rate.feeVnd() < 0 || request.shippingDiscountVnd() > rate.feeVnd()) throw new PaymentException(HttpStatus.UNPROCESSABLE_ENTITY, "GHN_RATE_INVALID", "GHN trả về phí giao hàng không hợp lệ.");
        ShippingQuote quote = new ShippingQuote(); quote.setId(UUID.randomUUID()); quote.setCustomerId(request.customerId()); quote.setProvinceId(request.provinceId()); quote.setWardId(request.wardId()); quote.setItemsFingerprint(itemsFingerprint(request)); quote.setRequestFingerprint(fingerprint); quote.setFeeVnd(rate.feeVnd()); quote.setShippingDiscountVnd(request.shippingDiscountVnd()); quote.setServiceId(rate.serviceId()); quote.setServiceName(rate.serviceName()); quote.setEtaText(rate.eta()); quote.setExpiresAt(now.plus(Duration.ofMinutes(properties.quoteTtlMinutes() == null ? 15 : properties.quoteTtlMinutes()))); quote.setCreatedAt(now); quotes.save(quote); return response(quote);
    }

    @Transactional
    public ShippingQuoteResponse validateAndUse(UUID quoteId, QuoteValidationRequest request) {
        ShippingQuote quote = quotes.findByIdAndCustomerId(quoteId, request.customerId()).orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "SHIPPING_QUOTE_NOT_FOUND", "Không tìm thấy báo giá giao hàng."));
        if (quote.getUsedAt() != null || !quote.getExpiresAt().isAfter(Instant.now()) || !quote.getRequestFingerprint().equals(request.requestFingerprint())) throw new PaymentException(HttpStatus.CONFLICT, "SHIPPING_QUOTE_INVALID", "Báo giá đã hết hạn, đã dùng hoặc không còn khớp với đơn hàng.");
        quote.setUsedAt(Instant.now()); quotes.save(quote); return response(quote);
    }

    private ShippingQuoteResponse response(ShippingQuote quote) { return new ShippingQuoteResponse(quote.getId(), quote.getFeeVnd(), quote.getShippingDiscountVnd(), quote.getFeeVnd() - quote.getShippingDiscountVnd(), quote.getServiceId(), quote.getServiceName(), quote.getEtaText(), quote.getExpiresAt()); }
    private String fingerprint(ShippingQuoteRequest request) { return sha256(Map.of("customerId", request.customerId(), "provinceId", request.provinceId(), "wardId", request.wardId(), "items", request.items(), "shippingDiscountVnd", request.shippingDiscountVnd(), "serviceCode", request.serviceCode() == null ? "" : request.serviceCode())); }
    private String itemsFingerprint(ShippingQuoteRequest request) { return sha256(request.items()); }
    private String sha256(Object value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(value))); } catch (Exception exception) { throw new IllegalStateException("Cannot fingerprint shipping quote", exception); } }
}
