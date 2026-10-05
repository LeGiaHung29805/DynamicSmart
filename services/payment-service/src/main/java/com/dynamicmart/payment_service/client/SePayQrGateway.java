package com.dynamicmart.payment_service.client;

import com.dynamicmart.payment_service.service.PaymentGatewayRouter;

import com.dynamicmart.payment_service.exception.PaymentException;
import com.dynamicmart.payment_service.config.SePayProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SePayQrGateway {
    private final SePayProperties properties;
    public SePayQrGateway(SePayProperties properties) { this.properties = properties; }

    public PaymentGatewayRouter.CreatedPayment create(UUID paymentId, long amountVnd, int attemptNo) {
        requireConfigured();
        String reference = "DM" + paymentId.toString().replace("-", "").substring(0, 16).toUpperCase() + attemptNo;
        String url = properties.qrBaseUrl() + "?acc=" + encode(properties.accountNo()) + "&bank=" + encode(properties.bankId())
                + "&amount=" + amountVnd + "&des=" + encode(reference);
        return new PaymentGatewayRouter.CreatedPayment(reference, url);
    }

    public boolean validWebhook(String rawBody, String timestamp, String signature) {
        if (!configured() || timestamp == null || signature == null) return false;
        try {
            long sentAt = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - sentAt) > 300) return false;
            String expected = "sha256=" + HmacSigner.sha256(properties.webhookSecret(), timestamp + "." + rawBody);
            return HmacSigner.equalsHex(expected, signature);
        } catch (NumberFormatException ignored) { return false; }
    }

    private void requireConfigured() { if (!configured()) throw new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "SEPAY_NOT_CONFIGURED", "QR ngân hàng/SePay chưa được cấu hình."); }
    private boolean configured() { return present(properties.webhookSecret()) && present(properties.bankId()) && present(properties.accountNo()) && present(properties.qrBaseUrl()); }
    private boolean present(String value) { return value != null && !value.isBlank(); }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
