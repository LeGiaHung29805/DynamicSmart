package com.dynamicmart.order_service.client;

import com.dynamicmart.order_service.api.OrderException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PaymentClient {
    private final RestClient client; private final String internalKey;
    public PaymentClient(RestClient.Builder builder, @Value("${app.clients.payment-service-url}") String baseUrl,
                         @Value("${app.security.internal-api-key}") String internalKey) {
        this.client = builder.baseUrl(baseUrl).build(); this.internalKey = internalKey;
    }
    public PaymentResponse create(UUID orderId, UUID customerId, long amount, String timing, String method, UUID correlationId) {
        try {
            PaymentResponse result = client.post().uri("/api/v1/payments/order-context")
                    .header("X-Internal-Api-Key", internalKey)
                    .body(new PaymentRequest(orderId, customerId, amount, timing, method, correlationId))
                    .retrieve().body(PaymentResponse.class);
            if (result == null) throw failed(); return result;
        } catch (RestClientException exception) { throw failed(); }
    }
    private OrderException failed() { return new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_CREATION_FAILED", "Không thể khởi tạo khoản thanh toán."); }
    private record PaymentRequest(UUID orderId, UUID customerId, long amountVnd, String timing, String method, UUID correlationId) { }
    public record PaymentResponse(UUID id, UUID orderId, long amountVnd, String timing, String method, String status,
                                  String redirectUrl, Instant expiresAt, Instant paidAt, String codReceiptNo,
                                  UUID codConfirmedBy, Instant codConfirmedAt) { }
}
