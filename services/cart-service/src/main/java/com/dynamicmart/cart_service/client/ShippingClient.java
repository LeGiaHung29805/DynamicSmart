package com.dynamicmart.cart_service.client;

import com.dynamicmart.cart_service.exception.CartException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ShippingClient {
    private final RestClient client; private final String internalKey;
    public ShippingClient(RestClient.Builder builder,
                          @Value("${app.clients.payment-service-url:http://localhost:8085}") String baseUrl,
                          @Value("${app.security.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(baseUrl).build(); this.internalKey = internalKey;
    }
    public ShippingQuote quote(UUID customerId, int provinceId, int wardId, List<ShippingItem> items, long discount) {
        try {
            ShippingQuote result = client.post().uri("/api/v1/shipping/quotes")
                    .header("X-Internal-Api-Key", internalKey)
                    .body(new QuoteRequest(customerId, provinceId, wardId, items, discount, null))
                    .retrieve().body(ShippingQuote.class);
            if (result == null) throw unavailable(); return result;
        } catch (RestClientException exception) { throw unavailable(); }
    }
    private CartException unavailable() { return new CartException(HttpStatus.SERVICE_UNAVAILABLE, "SHIPPING_QUOTE_UNAVAILABLE", "Không thể lấy báo giá GHN lúc này."); }
    private record QuoteRequest(UUID customerId, int provinceId, int wardId, List<ShippingItem> items,
                                long shippingDiscountVnd, String serviceCode) { }
    public record ShippingItem(UUID variantId, int quantity, int weightGrams, int lengthCm, int widthCm, int heightCm) { }
    public record ShippingQuote(UUID quoteId, long feeVnd, long shippingDiscountVnd, long payableFeeVnd,
                                int serviceId, String serviceName, String eta, Instant expiresAt) { }
}
