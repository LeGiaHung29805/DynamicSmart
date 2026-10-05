package com.dynamicmart.payment_service.client;

import com.dynamicmart.payment_service.service.PaymentGatewayRouter;

import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.config.PayOsProperties;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class PayOsGateway {
    private final PayOsProperties properties;
    private final RestClient restClient;

    public PayOsGateway(PayOsProperties properties, RestClient.Builder builder) { this.properties = properties; this.restClient = builder.build(); }

    public PaymentGatewayRouter.CreatedPayment create(UUID paymentId, long amountVnd, int attemptNo, Instant expiresAt) {
        requireConfigured();
        long orderCode = Math.floorMod(paymentId.getMostSignificantBits(), 90_000_000_000_000L) * 10 + Math.min(attemptNo, 9);
        String reference = Long.toString(orderCode);
        String description = "DM " + reference;
        String signed = "amount=" + amountVnd + "&cancelUrl=" + properties.cancelUrl() + "&description=" + description
                + "&orderCode=" + orderCode + "&returnUrl=" + properties.returnUrl();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode); body.put("amount", amountVnd); body.put("description", description);
        body.put("cancelUrl", properties.cancelUrl()); body.put("returnUrl", properties.returnUrl());
        body.put("expiredAt", expiresAt.getEpochSecond()); body.put("signature", HmacSigner.sha256(properties.checksumKey(), signed));
        try {
            JsonNode response = restClient.post().uri(properties.baseUrl() + "/v2/payment-requests")
                    .header("x-client-id", properties.clientId()).header("x-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            String url = response == null ? "" : response.path("data").path("checkoutUrl").asText("");
            if (url.isBlank()) throw unavailable();
            return new PaymentGatewayRouter.CreatedPayment(reference, url);
        } catch (RestClientException exception) { throw unavailable(); }
    }

    public boolean validWebhook(JsonNode data, String signature) {
        if (!configured() || data == null || !data.isObject() || signature == null) return false;
        java.util.TreeMap<String, String> values = new java.util.TreeMap<>();
        data.properties().forEach(entry -> values.put(entry.getKey(), entry.getValue().isNull() ? "" : entry.getValue().isValueNode() ? entry.getValue().asText() : entry.getValue().toString()));
        String canonical = values.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining("&"));
        return HmacSigner.equalsHex(HmacSigner.sha256(properties.checksumKey(), canonical), signature);
    }

    private boolean configured() { return present(properties.clientId()) && present(properties.apiKey()) && present(properties.checksumKey()) && present(properties.baseUrl()) && present(properties.returnUrl()) && present(properties.cancelUrl()); }
    private void requireConfigured() { if (!configured()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "PAYOS_NOT_CONFIGURED", "PayOS chưa được cấu hình."); }
    private boolean present(String value) { return value != null && !value.isBlank(); }
    private PaymentException unavailable() { return new PaymentException(HttpStatus.BAD_GATEWAY, "PAYOS_UNAVAILABLE", "PayOS không phản hồi. Vui lòng thử lại."); }
}
