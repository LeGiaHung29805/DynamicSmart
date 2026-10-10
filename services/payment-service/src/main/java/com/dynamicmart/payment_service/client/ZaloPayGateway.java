package com.dynamicmart.payment_service.client;

import com.dynamicmart.payment_service.service.PaymentGatewayRouter;

import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.config.ZaloPayProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class ZaloPayGateway {
    private final ZaloPayProperties properties;
    private final RestClient restClient;

    public ZaloPayGateway(ZaloPayProperties properties, RestClient.Builder builder) {
        this.properties = properties; this.restClient = builder.build();
    }

    public PaymentGatewayRouter.CreatedPayment create(UUID paymentId, UUID customerId, long amountVnd, int attemptNo) {
        requireConfigured();
        String day = DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.now());
        String reference = day + "_DM" + paymentId.toString().replace("-", "").substring(0, 18) + attemptNo;
        long appTime = System.currentTimeMillis();
        String embedData = "{\"redirecturl\":\"" + properties.returnUrl() + "\"}";
        String item = "[]";
        String data = String.join("|", properties.appId(), reference, customerId.toString(), Long.toString(amountVnd), Long.toString(appTime), embedData, item);
        var form = new LinkedMultiValueMap<String, String>();
        form.add("app_id", properties.appId()); form.add("app_user", customerId.toString()); form.add("app_time", Long.toString(appTime));
        form.add("amount", Long.toString(amountVnd)); form.add("app_trans_id", reference); form.add("embed_data", embedData); form.add("item", item);
        form.add("description", "DynamicMart " + reference); form.add("bank_code", ""); form.add("callback_url", properties.callbackUrl());
        form.add("mac", HmacSigner.sha256(properties.key1(), data));
        try {
            JsonNode response = restClient.post().uri(properties.createUrl()).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
            String url = response == null ? "" : response.path("order_url").asText("");
            if (url.isBlank()) throw unavailable();
            return new PaymentGatewayRouter.CreatedPayment(reference, url);
        } catch (RestClientException exception) { throw unavailable(); }
    }

    public boolean validCallback(String data, String mac) {
        return isConfigured() && HmacSigner.equalsHex(HmacSigner.sha256(properties.key2(), data), mac);
    }

    public boolean isConfigured() { return present(properties.appId()) && present(properties.key1()) && present(properties.key2()) && present(properties.createUrl()) && present(properties.callbackUrl()) && present(properties.returnUrl()); }
    private void requireConfigured() { if (!isConfigured()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "ZALOPAY_NOT_CONFIGURED", "ZaloPay chưa được cấu hình."); }
    private boolean present(String value) { return value != null && !value.isBlank(); }
    private PaymentException unavailable() { return new PaymentException(HttpStatus.BAD_GATEWAY, "ZALOPAY_UNAVAILABLE", "ZaloPay không phản hồi. Vui lòng thử lại."); }
}
